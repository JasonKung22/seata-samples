package io.seata.order.service;

import io.seata.order.entity.Order;
import io.seata.order.mapper.core.OrderCoreMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCoreService {


    @Resource
    private OrderCoreMapper orderCoreMapper;

    public void insert(Order order) {
        orderCoreMapper.insert(order);
    }
}
