package z.hong.shoppayweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import z.hong.shopapi.api.IPayService;
import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TradePay;

@RestController
@RequestMapping("/pay")
public class PayController {

    @Reference
    private IPayService payService;

    @RequestMapping("/createPayment")
    public Result createPayment(@RequestBody TradePay pay){
            return payService.createPayment(pay);
    }
    @RequestMapping("/callbackPayment")
    public Result callBackPayment(@RequestBody TradePay pay){
        return payService.callbackPayment(pay);
    }
}
