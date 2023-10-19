# 一、前言
> 该方案基于seata官方示例进行修改，整合了spring-cloud-alibaba + seata + sharding-jdbc + mybatis + 多数据源，去除了mybatis-plus、sharding-transaction-base-seata-at依赖，采用手动配置seata和sharding-jdbc数据源的方式进行整合，通过@GlobalTransactional进行分布式事务的使用，和分库分表前的seata使用上没有区别。
> **注意**：sharding-jdbc不同版本之间配置和设计差异较大，请合理选择sharding-jdbc版本

seata：1.4.2
sharding-jdbc：4.1.1
spring-boot：2.3.5
spring-cloud：Hoxton.SR9
spring-cloud-alibaba：2.2.5.RELEASE

原示例源码：[https://github.com/seata/seata-samples/tree/master/springcloud-seata-sharding-jdbc-mybatis-plus-samples](https://github.com/seata/seata-samples/tree/master/springcloud-seata-sharding-jdbc-mybatis-plus-samples)

本文档源码：[https://github.com/JasonKung22/seata-samples/tree/master/springcloud-seata-sharding-jdbc-mybatis-plus-samples](https://github.com/JasonKung22/seata-samples/tree/master/springcloud-seata-sharding-jdbc-mybatis-plus-samples)
# 二、原理剖析
### 1、剖析Seata分布式事务实现原理
##### 数据源自动代理
seata开启自动代理数据源之后，每次注册dataSourceBean之后，SeataDataSourceBeanPostProcessor都会自动对数据源进行代理![image.png](https://cdn.nlark.com/yuque/0/2023/png/1221070/1697181968273-0e83965e-7bfa-45b9-8a6e-ef72d3c04084.png#averageHue=%232d2c2b&clientId=ubf709e40-b352-4&from=paste&height=781&id=u39fb7f96&originHeight=781&originWidth=1077&originalType=binary&ratio=1&rotation=0&showTitle=false&size=115606&status=done&style=none&taskId=u032cf2e6-29ea-4830-8b1b-a5003c6d767&title=&width=1077)
##### 注册全局事务
当调用微服务A的@GlobalTransactional注解的方法时，会向TC注册全局事务，并得到全局事务ID（xid）
![image.png](https://cdn.nlark.com/yuque/0/2023/png/1221070/1697622186029-6c706cb3-2685-4d31-8a0b-8862959d2d31.png#averageHue=%23685f4b&clientId=u1d6a4295-e854-4&from=paste&id=ud6a179ae&originHeight=482&originWidth=853&originalType=url&ratio=1&rotation=0&showTitle=false&size=40647&status=done&style=none&taskId=u3f46890a-d304-482e-909b-77c74dff982&title=)
##### 微服务间事务传递
当微服务A调用微服务B时，SeataRestTemplateInterceptor会将xid放入请求Header中传入微服务B，微服务B通过SeataHandlerInterceptor将请求Header中加载到当前线程中![image.png](https://cdn.nlark.com/yuque/0/2023/png/1221070/1697182056033-8f552a15-da4c-4675-a5bc-0da47c4ea1ad.png#averageHue=%232d2c2b&clientId=ubf709e40-b352-4&from=paste&height=331&id=ue1e33377&originHeight=331&originWidth=1254&originalType=binary&ratio=1&rotation=0&showTitle=false&size=43185&status=done&style=none&taskId=u8fdd3b63-a05b-4806-a8a8-6431188cc68&title=&width=1254)
![image.png](https://cdn.nlark.com/yuque/0/2023/png/1221070/1697182042266-af31deb3-73e1-4a6d-bd95-1cc826098754.png#averageHue=%232c2c2b&clientId=ubf709e40-b352-4&from=paste&height=530&id=u8e39f4c6&originHeight=530&originWidth=1011&originalType=binary&ratio=1&rotation=0&showTitle=false&size=64383&status=done&style=none&taskId=u875e1ff2-20f7-469b-827f-13fc6f5ba0b&title=&width=1011)
#####  是否添加 @Transactional 的区别

1. 如果有@Transactional注解，会在本地事务提交时，向TC注册分支事务
2. 如果没有@Transactional注解，会每次执行完SQL后，都向TC注册分支事务（如果一个方法中有3个数据操作，就会向TC注册3个分支事务）
##### 提交或回滚全局事务

1. 当微服务A的@GlobalTransactional方法正常执行完，TM会通知TC全局事务成功，TC再通知所有分支事务进行二阶段提交
2. 当微服务A的@GlobalTransactional方法抛出异常时，TM会通知TC全局事务失败，TC再通知所有分支事务进行二阶段回滚

![](https://cdn.nlark.com/yuque/0/2023/png/1221070/1697167763780-5839e318-c487-4aca-929b-0078eb5f2bba.png#averageHue=%23fbeabd&clientId=ubf709e40-b352-4&from=paste&id=u0c2cc752&originHeight=908&originWidth=1534&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=ubf1cfc7e-2b41-4aa3-9560-8990bf2a45c&title=)
### 2、剖析ShardingJDBC实现原理
##### 分片规则自动配置
在yaml中spring.shardingsphere.sharding配置分片规则后，会加载到SpringBootShardingRuleConfigurationProperties中![image.png](https://cdn.nlark.com/yuque/0/2023/png/1221070/1697182779273-41746ffd-e002-4228-9852-9a32da2b6400.png#averageHue=%232d2c2c&clientId=ubf709e40-b352-4&from=paste&height=139&id=u1a52f9ae&originHeight=139&originWidth=897&originalType=binary&ratio=1&rotation=0&showTitle=false&size=16333&status=done&style=none&taskId=u722a27a6-9596-4bef-af18-3874c17b689&title=&width=897)
##### 数据源自动配置
在yaml中spring.shardingsphere.datasource配置数据源后，org.apache.shardingsphere.shardingjdbc.spring.boot.SpringBootConfiguration会自动创建ShardingDataSource，并注入分片规则等配置![image.png](https://cdn.nlark.com/yuque/0/2023/png/1221070/1697182511435-a4ea43e3-f8a1-466a-ba71-20a38e9cccfc.png#averageHue=%232d2c2b&clientId=ubf709e40-b352-4&from=paste&height=759&id=AAcmm&originHeight=759&originWidth=1262&originalType=binary&ratio=1&rotation=0&showTitle=false&size=126989&status=done&style=none&taskId=u389648d1-c605-44f8-a1ca-f118b0bc0f6&title=&width=1262)
##### 引入seata分布式事务
引入sharding-transaction-base-seata-at依赖后，SeataATShardingTransactionManager会自动对ShardingDataSource中的实际数据源创建seata数据源代理![image.png](https://cdn.nlark.com/yuque/0/2023/png/1221070/1697181720736-d42d5f57-ed11-48c8-892f-f4cbe505c0ae.png#averageHue=%232d2c2c&clientId=ubf709e40-b352-4&from=paste&height=598&id=u90c39548&originHeight=598&originWidth=1037&originalType=binary&ratio=1&rotation=0&showTitle=false&size=95274&status=done&style=none&taskId=u7db9ae8d-c186-4459-9e7a-7de61be61ac&title=&width=1037)
##### 执行流程
当service调用dao时，根据SqlSessionFactory的MapperLocations配置识别到logicDateSource，然后根据分片规则找到实际数据源，完成以下流程图的SQL执行![](https://cdn.nlark.com/yuque/0/2023/png/1221070/1697167735226-65129b6b-008b-450a-83d1-636cd04c12b9.png#averageHue=%23b4c48d&clientId=ubf709e40-b352-4&from=paste&id=zjU3O&originHeight=936&originWidth=1550&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u388d8c41-8918-4c3b-9fb0-a3393f9f20b&title=)
# 三、设计思路
### 1、基于~~@ShardingTransactionType~~（5.4.0版本已弃用）
> 该方式在ShardingJDBC5.4.0版本前，为官方推荐，网上大多数也是基于此方案进行配置。但经过实测存在其它一些问题，不满足我们的应用场景

按照官方文档对接即可，不需要进行设计，对接中发现的问题如下：

1. 不支持事务传播，如果微服务A和微服务B都引入了shardingJDBC，当微服务A调用微服务B时，微服务B的方法上面没有加@ShardingTransactionType，微服务B的不会加入到全局事务中
2. 自动配置对多数据源支持不友好，需要自行创建ShardingDataSource，并注入到SqlSessionFactoryBean中
3. @ShardingTransactionType和@GlobalTransactional不能混用，可能出现未知问题，比如空指针[https://github.com/apache/shardingsphere/issues/22356](https://github.com/apache/shardingsphere/issues/22356)
### 2、基于@GlobalTransactional（推荐）
> 由于seata和shardingJDBC本质上都是对数据源进行代理，我们只需要将shardingJDBC逻辑数据源下的每一个实际数据源当成多数据源，分片成功之后，就可以确定某一个数据源，再像分库分表前一样使用seata就可以了。总体执行流程：
> 分库分表前：mybatis > seataDataSource > druidDataSource > connection
> 分库分表后：mybatis > shardingDataSource > seataDataSource > druidDataSource > connection

1. 为了配置多数据源，需要禁用spring默认的数据源自动配置
2. 为了达到上诉的执行流程，需要自定义数据源和代理数据源，因此也需要禁用sharding、seata数据源自动配置
3. 由于已经禁用了spring、seata、sharding的数据源自动配置，所以需要程序自定义数据源并进行管理。为了进行区分，将数据源定义在spring.database下
# 四、具体实现
### 1、基于~~@ShardingTransactionType~~
##### 引入分布式事务seata依赖
微服务项目需要同时spring-cloud-starter-alibaba-seata依赖，不然微服务间调用时，xid不会进行传递
```xml
<dependency>
  <groupId>org.apache.shardingsphere</groupId>
  <artifactId>sharding-transaction-base-seata-at</artifactId>
  <version>${sharding-sphere.version}</version>
</dependency>
<dependency>
  <groupId>com.alibaba.cloud</groupId>
  <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
```
##### 添加seata.config配置
需要将该配置放在classpath下
```
client {
    application.id = order-server
    transaction.service.group = seata-group
}
```
##### 关闭seata的数据源自动代理
```yaml
seata:
  enable-auto-data-source-proxy: false
```
##### 使用@ShardingTransactionType
在需要分布式事务的方法上，加上@ShardingTransactionType(TransactionType.BASE)就可以正常使用了（但是ShardingTransactionType缺少事务的传播机制，需要注意加@ShardingTransactionType注解）
### 2、基于@GlobalTransactional
##### 关闭seata自动代理数据源
```yaml
seata:
  enable-auto-data-source-proxy: false
```
##### 排除spring、shardingJDBC数据源自动配置
```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, SpringBootConfiguration.class})
```
##### 自定义shardingJDBC自动配置
```java
@Configuration
@ComponentScan("org.apache.shardingsphere.spring.boot.converter")
@EnableConfigurationProperties({
        SpringBootShardingRuleConfigurationProperties.class,
        SpringBootMasterSlaveRuleConfigurationProperties.class, SpringBootEncryptRuleConfigurationProperties.class,
        SpringBootPropertiesConfigurationProperties.class, SpringBootShadowRuleConfigurationProperties.class})
@ConditionalOnProperty(prefix = "spring.shardingsphere", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@RequiredArgsConstructor
public class CustomShardingSphereAutoConfiguration {


    /**
     * Create transaction type scanner.
     *
     * @return transaction type scanner
     */
    @Bean
    public ShardingTransactionTypeScanner transactionTypeScanner() {
        return new ShardingTransactionTypeScanner();
    }
}
```
##### 配置多数据源
在yaml中spring.database下配置数据源
```yaml
spring:
  database:
    ds0:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://192.168.0.7:3308/seata_order_0?serverTimezone=UTC&characterEncoding=utf8
      username: root
      password: 123456
      initial-size: 10
      max-active: 15
      min-idle: 10
      max-wait: 60000
    ds1:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://192.168.0.7:3308/seata_order_1?serverTimezone=UTC&characterEncoding=utf8
      username: root
      password: 123456
      initial-size: 10
      max-active: 15
      min-idle: 10
      max-wait: 60000
```
##### 配置分片规则
在yaml中spring.shardingsphere下配置分片规则等配置（该方案适用于一个微服务只有一个shardingDataSource，如果需要有多个，也需要自定义分片规则参数）
```yaml
spring:
  shardingsphere:
    props:
      sql:
        show: true
    sharding:
      default-data-source-name: ds0
      tables:
        order_info:
          actual-data-nodes: ds$->{0..1}.order_info_$->{0..2}
          database-strategy:
            inline:
              algorithm-expression: ds$->{id % 2}
              sharding-column: id
          table-strategy:
            inline:
              algorithm-expression: order_info_$->{id % 3}
              sharding-column: id
```
##### 注册并代理数据源
自定义druidDataSource、seataDataSource、shardingDataSource并注入到SqlSessionFactory和TransactionManager里面
```java
@Configuration
@MapperScan(basePackages = "io.seata.order.mapper", sqlSessionFactoryRef = "dsSqlSessionFactory")
@RequiredArgsConstructor
public class DataSourceConfig {

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

    public Map<String, DataSource> getDataSourceMap(DataSource controlLogDataSource, DataSource controlLog2DataSource) {
        Map<String,DataSource> dataSourceMap = new HashMap<>();
        dataSourceMap.put("ds0", controlLogDataSource);
        dataSourceMap.put("ds1", controlLog2DataSource);
        return dataSourceMap;
    }
```
##### 多数据源配置方式和上面一样，源码中有所体现
# 五、参考资料
[Seata官方博客-Seata数据源代理解析](https://seata.io/zh-cn/blog/seata-datasource-proxy/)


# 项目启动流程

一、启动nacos（demo版本1.4.0），导入配置文件，配置文件在项目sql-and-seataconfig文件夹中。

二、启动seata（demo版本1.3.0，其实1.4.2也支持），将seata配置registry.conf中配置中心和注册中心都改成nacos。

三、启动demo。

​ POST访问http://localhost:8001/seata/test?hasError=false，订单和产品都成功，日志也有提交信息。

​ POST访问http://localhost:8001/seata/test?hasError=true，订单和产品都失败，日志也有回滚信息。