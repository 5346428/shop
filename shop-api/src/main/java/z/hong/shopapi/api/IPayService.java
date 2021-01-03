package z.hong.shopapi.api;

import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TradePay;

public interface IPayService {
    Result createPayment(TradePay tradePay);
    Result callbackPayment(TradePay tradePay);
}
