package io.seata.product.service;

import cn.hutool.core.util.IdUtil;
import io.seata.product.client.OrderClient;
import io.seata.product.entity.Product;
import io.seata.product.entity.ProductRecord;
import io.seata.product.mapper.ProductMapper;
import io.seata.product.mapper.ProductRecordMapper;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductRecordMapper productRecordMapper;

    @Autowired
    private OrderClient orderClient;

    @Transactional
    public void minusStock() {
        productMapper.minusStock();

        ProductRecord product = new ProductRecord();
        product.setId(IdUtil.getSnowflake().nextId());
        product.setProductId(1L);
        product.setProductName("测试产品");
        product.setBuyNum(1);
        productRecordMapper.insertRecord(product);
    }

    @GlobalTransactional
    public void seataToSharding(Boolean hasError) {
        productMapper.minusStock();

        ProductRecord product = new ProductRecord();
        product.setId(IdUtil.getSnowflake().nextId());
        product.setProductId(1L);
        product.setProductName("测试产品");
        product.setBuyNum(1);
        productRecordMapper.insertRecord(product);

        orderClient.seataToSharding(false);

        //异常模拟
        if (hasError) {
            int i = 1 / 0;
        }
    }
}
