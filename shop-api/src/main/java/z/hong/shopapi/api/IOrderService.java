package z.hong.shopapi.api;

import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TradeOrder;

public interface IOrderService {

    /**
     * 下单接口
     * @param order
     * @return
     */
    Result confirmOrder(TradeOrder order);
}
