package io.seata.order.service;

import cn.hutool.core.util.IdUtil;
import io.seata.order.client.ProductClient;
import io.seata.order.entity.Order;
import io.seata.order.mapper.ds.OrderItemMapper;
import io.seata.order.mapper.ds.OrderMapper;
import io.seata.spring.annotation.GlobalTransactional;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    @Resource
    private ProductClient productClient;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private OrderCoreService orderCoreService;

    @Resource
    private OrderService orderService;


    @GlobalTransactional
    public void seataDemo(Boolean hasError) {
        //下单操作
        Order order = new Order();
        order.setId(IdUtil.getSnowflake().nextId());
        order.setOrderName("测试数据");
        order.setBuyNum(2);
        // DS数据源
        orderService.insert(order);
        // core数据源
        orderCoreService.insert(order);

        //减库存（这里参数什么的就自己脑补了）
        productClient.minusStock();

        //异常模拟
        if (hasError) {
            int i = 1 / 0;
        }
    }

    public void seataToSharding(Boolean hasError) {
        //下单操作
        Order order = new Order();
        order.setId(IdUtil.getSnowflake().nextId());
        order.setOrderName("测试数据");
        order.setBuyNum(2);
        orderService.insert(order);
        orderCoreService.insert(order);
        //异常模拟
        if (hasError) {
            int i = 1 / 0;
        }
    }

    @Transactional(rollbackFor = Exception.class, value = "dsTransactionManager")
    public void insert(Order order){
        orderMapper.insert(order);
        orderItemMapper.insert(order);
    }
}
