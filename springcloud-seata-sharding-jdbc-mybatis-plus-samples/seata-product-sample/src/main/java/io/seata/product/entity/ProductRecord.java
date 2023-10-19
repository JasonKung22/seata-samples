package io.seata.product.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductRecord {
    private Long id;

    private Long productId;

    private String productName;

    private Integer buyNum;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
