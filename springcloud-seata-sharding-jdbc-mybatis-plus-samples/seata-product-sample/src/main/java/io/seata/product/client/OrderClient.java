package io.seata.product.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("order-server")
public interface OrderClient {

    @PostMapping("/seataToSharding")
    Void seataToSharding(@RequestParam Boolean hasError);

}
