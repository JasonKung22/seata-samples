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
@MapperScan(basePackages = "io.seata.order.mapper.core", sqlSessionFactoryRef = "coreSqlSessionFactory")
@RequiredArgsConstructor
public class CoreDataSourceConfig {

    @Bean(name = "coreDataSource")
    @ConfigurationProperties(prefix = "spring.database.core")
    public DataSource coreDataSource() {
        DruidDataSource druidDataSource = DruidDataSourceBuilder.create().build();
        druidDataSource.setTestWhileIdle(true);
        druidDataSource.setValidationQuery("SELECT 1");
        return druidDataSource;
    }


    @Bean(name = "coreSeataDatasource")
    public DataSourceProxy coreSeataDatasource(@Qualifier("coreDataSource") DataSource coreDataSource) {
        return new DataSourceProxy(coreDataSource);
    }

    @Bean(name = "coreSqlSessionFactory")
    public SqlSessionFactory logSqlSessionFactory(
            @Qualifier("coreSeataDatasource") DataSource coreSeataDatasource)
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(coreSeataDatasource);
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapping/core/*.xml"));
        bean.setVfs(SpringBootVFS.class);
        return bean.getObject();
    }

    @Bean(name = "coreSqlSessionTemplate")
    public SqlSessionTemplate logSqlSessionTemplate(
            @Qualifier("coreSqlSessionFactory") SqlSessionFactory coreSqlSessionFactory) {
        return new SqlSessionTemplate(coreSqlSessionFactory);
    }

    @Primary
    @Bean(name = "coreTransactionManager")
    public DataSourceTransactionManager controlLogTransactionManager(
            @Qualifier("coreSeataDatasource") DataSource coreSeataDatasource) {
        return new DataSourceTransactionManager(coreSeataDatasource);
    }
}
