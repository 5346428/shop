package z.hong.shopapi.api;

import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TradeGoods;
import z.hong.shoppojo.pojo.TradeGoodsNumberLog;

public interface IGoodsService {

    /**
     * 根据id查询商品
     * @param id
     * @return
     */
    TradeGoods findOne(Long id);

    /**
     * 扣减库存
     */
    Result reduceGoodsNum(TradeGoodsNumberLog tradeGoodsNumberLog);
}
