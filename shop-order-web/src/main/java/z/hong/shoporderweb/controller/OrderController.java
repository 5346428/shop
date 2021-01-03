package z.hong.shoporderweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import z.hong.shopapi.api.IOrderService;
import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TradeOrder;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Reference
    private IOrderService orderService;
    private Logger log= LoggerFactory.getLogger(OrderController.class);

    @RequestMapping("/confirmOrder")
    public Result confirmOrder(@RequestBody TradeOrder order){
            log.info("order-web");
            log.info(JSONObject.toJSONString(order));
            return orderService.confirmOrder(order);
    }
}
