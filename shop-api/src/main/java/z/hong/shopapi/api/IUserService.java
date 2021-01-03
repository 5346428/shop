package z.hong.shopapi.api;

import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TradeOrder;
import z.hong.shoppojo.pojo.TradeUser;
import z.hong.shoppojo.pojo.TradeUserMoneyLog;

public interface IUserService {
    TradeUser findOne(Long userId);

    /**
     * 更新余额
     * @param order
     * @return
     */
    Result updateMoneyPaid(TradeUserMoneyLog order);
}
