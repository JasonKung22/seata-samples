package io.seata.order.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Order {

    private Long id;

    private String orderName;

    private Long productId;

    private Integer buyNum;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
