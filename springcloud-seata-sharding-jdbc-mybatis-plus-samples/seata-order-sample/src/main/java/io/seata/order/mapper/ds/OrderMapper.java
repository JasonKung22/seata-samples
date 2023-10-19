package io.seata.order.mapper.ds;

import io.seata.order.entity.Order;

public interface OrderMapper {
    void insert(Order order);
}
