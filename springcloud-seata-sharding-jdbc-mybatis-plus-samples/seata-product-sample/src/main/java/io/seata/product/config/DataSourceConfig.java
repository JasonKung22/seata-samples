package io.seata.product.config;

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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * 数据源配置
 * @author gyf
 * @date 2023/10/10 10:35
 */
@Configuration
@MapperScan(basePackages = "io.seata.product.mapper", sqlSessionFactoryRef = "productSqlSessionFactory")
@RequiredArgsConstructor
public class DataSourceConfig {

    private final SpringBootShardingRuleConfigurationProperties shardingRule;

    private final SpringBootPropertiesConfigurationProperties props;

    @Bean(name = "productDataSource")
    @ConfigurationProperties(prefix = "spring.database.product")
    public DataSource ds0DataSource() {
        DruidDataSource druidDataSource = DruidDataSourceBuilder.create().build();
        druidDataSource.setTestWhileIdle(true);
        druidDataSource.setValidationQuery("SELECT 1");
        return druidDataSource;
    }


    @Bean(name = "productSeataDatasource")
    public DataSourceProxy ds0SeataDatasource(@Qualifier("productDataSource") DataSource productDataSource) {
        return new DataSourceProxy(productDataSource);
    }


    @Bean(name = "productShardingDataSource")
    @Conditional(ShardingRuleCondition.class)
    public DataSource dsShardingDataSource(@Qualifier("productSeataDatasource") DataSource productSeataDatasource) throws SQLException {
        return ShardingDataSourceFactory.createDataSource(getDataSourceMap(productSeataDatasource),
                new ShardingRuleConfigurationYamlSwapper().swap(shardingRule), props.getProps());
    }

    @Bean(name = "productSqlSessionFactory")
    public SqlSessionFactory logSqlSessionFactory(
            @Qualifier("productShardingDataSource") DataSource productShardingDataSource)
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(productShardingDataSource);
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapping/product/*.xml"));
        bean.setVfs(SpringBootVFS.class);
        return bean.getObject();
    }

    @Bean(name = "productSqlSessionTemplate")
    public SqlSessionTemplate logSqlSessionTemplate(
            @Qualifier("productSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "productTransactionManager")
    public DataSourceTransactionManager controlLogTransactionManager(
            @Qualifier("productShardingDataSource") DataSource productShardingDataSource) {
        return new DataSourceTransactionManager(productShardingDataSource);
    }

    public Map<String, DataSource> getDataSourceMap(DataSource productDataSource) {
        Map<String,DataSource> dataSourceMap = new HashMap<>();
        dataSourceMap.put("product", productDataSource);
        return dataSourceMap;
    }
}
