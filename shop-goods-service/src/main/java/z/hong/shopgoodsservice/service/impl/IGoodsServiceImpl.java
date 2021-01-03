package z.hong.shopgoodsservice.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import z.hong.shopapi.api.IGoodsService;
import z.hong.shopcommon.constant.ShopCode;
import z.hong.shopcommon.exception.CastException;
import z.hong.shopgoodsservice.mapper.TradeGoodsMapper;
import z.hong.shopgoodsservice.mapper.TradeGoodsNumberLogMapper;
import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TradeGoods;
import z.hong.shoppojo.pojo.TradeGoodsNumberLog;

@Component
@Service(interfaceClass = IGoodsService.class)
public class IGoodsServiceImpl implements IGoodsService {

    @Autowired
    TradeGoodsMapper tradeGoodsMapper;

    @Autowired
    TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @Override
    public TradeGoods findOne(Long id) {
        if(id==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return tradeGoodsMapper.selectByPrimaryKey(id);
    }

    @Override
    public Result reduceGoodsNum(TradeGoodsNumberLog goodsNumberLog) {
        if(goodsNumberLog==null||goodsNumberLog.getGoodsNumber()==null
                ||goodsNumberLog.getGoodsId()==null||goodsNumberLog.getOrderId()==null
                ||goodsNumberLog.getGoodsNumber().intValue()<0){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        TradeGoods tradeGoods = tradeGoodsMapper.selectByPrimaryKey(goodsNumberLog.getGoodsId());
        if(tradeGoods.getGoodsNumber()<goodsNumberLog.getGoodsNumber()){
                //库存不足
                CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }
        //减库存
        tradeGoods.setGoodsNumber(tradeGoods.getGoodsNumber()-goodsNumberLog.getGoodsNumber());
        tradeGoodsMapper.updateByPrimaryKey(tradeGoods);
        //记录日志
        goodsNumberLog.setGoodsNumber(-(goodsNumberLog.getGoodsNumber()));
        goodsNumberLogMapper.insert(goodsNumberLog);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
