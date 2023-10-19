package io.seata.order.mapper.ds;

import io.seata.order.entity.Order;

public interface OrderItemMapper {
    void insert(Order order);
}
