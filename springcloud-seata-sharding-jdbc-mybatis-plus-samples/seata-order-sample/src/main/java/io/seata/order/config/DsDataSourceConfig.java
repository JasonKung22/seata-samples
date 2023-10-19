package io.seata.order.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import io.seata.rm.datasource.DataSourceProxy;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.shardingsphere.core.yaml.swapper.ShardingRuleConfigurationYamlSwapper;
import org.apache.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;
import org.apache.shardingsphere.shardingjdbc.spring.boot.common.SpringBootPropertiesConfigurationProperties;
import org.apache.shardingsphere.shardingjdbc.spring.boot.sharding.ShardingRuleCondition;
import org.apache.shardingsphere.shardingjdbc.spring.boot.sharding.SpringBootShardingRuleConfigurationProperties;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * 数据源配置
 * @author gyf
 * @date 2023/10/10 10:35
 */
@Configuration
@MapperScan(basePackages = "io.seata.order.mapper.ds", sqlSessionFactoryRef = "dsSqlSessionFactory")
@RequiredArgsConstructor
public class DsDataSourceConfig {

    private final SpringBootShardingRuleConfigurationProperties shardingRule;

    private final SpringBootPropertiesConfigurationProperties props;

    @Bean(name = "ds0DataSource")
    @ConfigurationProperties(prefix = "spring.database.ds0")
    public DataSource ds0DataSource() {
        DruidDataSource druidDataSource = DruidDataSourceBuilder.create().build();
        druidDataSource.setTestWhileIdle(true);
        druidDataSource.setValidationQuery("SELECT 1");
        return druidDataSource;
    }

    @Bean(name = "ds1DataSource")
    @ConfigurationProperties(prefix = "spring.database.ds1")
    public DataSource ds1DataSource() {
        DruidDataSource druidDataSource = DruidDataSourceBuilder.create().build();
        druidDataSource.setTestWhileIdle(true);
        druidDataSource.setValidationQuery("SELECT 1");
        return druidDataSource;
    }


    @Bean(name = "ds0SeataDatasource")
    public DataSourceProxy ds0SeataDatasource(@Qualifier("ds0DataSource") DataSource ds0DataSource) {
        return new DataSourceProxy(ds0DataSource);
    }


    @Bean(name = "ds1SeataDatasource")
    public DataSourceProxy ds1SeataDatasource(@Qualifier("ds1DataSource") DataSource ds1DataSource) {
        return new DataSourceProxy(ds1DataSource);
    }


    @Bean(name = "dsShardingDataSource")
    @Conditional(ShardingRuleCondition.class)
    public DataSource dsShardingDataSource(@Qualifier("ds0SeataDatasource") DataSource ds0SeataDatasource,
            @Qualifier("ds1SeataDatasource") DataSource ds1SeataDatasource) throws SQLException {
        return ShardingDataSourceFactory.createDataSource(getDataSourceMap(ds0SeataDatasource, ds1SeataDatasource),
                new ShardingRuleConfigurationYamlSwapper().swap(shardingRule), props.getProps());
    }

    @Bean(name = "dsSqlSessionFactory")
    public SqlSessionFactory logSqlSessionFactory(
            @Qualifier("dsShardingDataSource") DataSource dsShardingDataSource)
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dsShardingDataSource);
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapping/ds/*.xml"));
        bean.setVfs(SpringBootVFS.class);
        return bean.getObject();
    }

    @Bean(name = "dsSqlSessionTemplate")
    public SqlSessionTemplate logSqlSessionTemplate(
            @Qualifier("dsSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "dsTransactionManager")
    public DataSourceTransactionManager controlLogTransactionManager(
            @Qualifier("dsShardingDataSource") DataSource dsShardingDataSource) {
        return new DataSourceTransactionManager(dsShardingDataSource);
    }

    public Map<String, DataSource> getDataSourceMap(DataSource ds0SeataDatasource, DataSource ds1SeataDatasource) {
        Map<String,DataSource> dataSourceMap = new HashMap<>();
        dataSourceMap.put("ds0", ds0SeataDatasource);
        dataSourceMap.put("ds1", ds1SeataDatasource);
        return dataSourceMap;
    }
}
