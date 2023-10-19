package io.seata.order.mapper.core;

import io.seata.order.entity.Order;

public interface OrderCoreMapper {
    void insert(Order order);
}
