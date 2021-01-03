package z.hong.shoporderservice.service.impl;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import z.hong.shopapi.api.ICouponService;
import z.hong.shopapi.api.IGoodsService;
import z.hong.shopapi.api.IOrderService;
import z.hong.shopapi.api.IUserService;
import z.hong.shopcommon.constant.ShopCode;
import z.hong.shopcommon.exception.CastException;
import z.hong.shopcommon.utils.IDWorker;
import z.hong.shoporderservice.mapper.TradeOrderMapper;
import z.hong.shoppojo.entity.MQEntity;
import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.*;

import java.math.BigDecimal;
import java.util.Date;


@Component
@Service(interfaceClass =IOrderService.class )
public class IOrderServiceImpl implements IOrderService {

    @Autowired
    private IDWorker idWorker;
    @Reference
    IGoodsService goodsService;
    @Reference
    IUserService userService;
    @Reference
    ICouponService couponService;
    @Autowired
    private TradeOrderMapper tradeOrderMapper;
    @Autowired
    private RocketMQTemplate mqTemplate;

    @Value("${mq.order.topic}")
    private String topic;
    @Value("${mq.order.tag.cancel}")
    private String tag;
    private Logger logger= LoggerFactory.getLogger(IOrderServiceImpl.class);

    @Override
    public Result confirmOrder(TradeOrder order) {

        //1.校验订单
        checkOrder(order);
        //2.生成预订单
        Long orderId = savePreOrder(order);
        try{
            //3.扣减库存
            reduceGoodsNum(order);
            //4.扣减优惠卷
            updateCouponStatus(order);
            //5.使用余额
            reduceMoneyPaid(order);
            CastException.cast(ShopCode.SHOP_FAIL);
            //6.确认订单
            updateOrderStatus(order);
            //7.返回成功状态
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
        }catch (Exception e){
            //1.确认订单失败，发送信息
            //订单ID 优惠卷ID 用户ID 商品ID 余额 商品数量
            MQEntity mqEntity = new MQEntity();
            mqEntity.setCouponId(order.getCouponId());
            mqEntity.setGoodsId(order.getGoodsId());
            mqEntity.setUserMoney(order.getMoneyPaid());
            mqEntity.setOrderId(order.getOrderId());
            mqEntity.setGoodsNum(order.getGoodsNumber());
            mqEntity.setUserId(order.getUserId());

            try {
                senCancelOrder(topic,tag,orderId.toString(), JSONObject.toJSONString(mqEntity));
                logger.info("发送订单回退消息");
            } catch (Exception exception) {
                exception.printStackTrace();
                logger.error(exception.getMessage());
            }
            //2.返回失败状态
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
        }
    }

    /**
     * 发送订单失败消息
     * @param topic
     * @param tag
     * @param keys
     * @param body
     */
    private void senCancelOrder(String topic, String tag, String keys, String body) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        Message message = new Message(topic, tag, keys, body.getBytes());
        mqTemplate.getProducer().send(message);
    }

    /**
     * 更新订单状态(确认订单)
     * @param order
     */
    private void updateOrderStatus(TradeOrder order) {
        order.setOrderStatus(ShopCode.SHOP_ORDER_CONFIRM.getCode());
        order.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
        order.setConfirmTime(new Date());
        int i = tradeOrderMapper.updateByPrimaryKey(order);
        if(i<=0){
            CastException.cast(ShopCode.SHOP_ORDER_CONFIRM_FAIL);
        }
        logger.info("订单"+order.getOrderId()+",确认订单成功");
    }

    /**
     * 使用余额
     * @param order
     */
    private void reduceMoneyPaid(TradeOrder order) {
        if(order.getMoneyPaid()!=null&&order.getMoneyPaid().compareTo(BigDecimal.ZERO)==1){
            TradeUserMoneyLog userMoneyLog = new TradeUserMoneyLog();
            userMoneyLog.setOrderId(order.getOrderId());
            userMoneyLog.setUserId(order.getUserId());
            userMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_PAID.getCode());
            userMoneyLog.setUseMoney(order.getMoneyPaid());
            Result result=userService.updateMoneyPaid(userMoneyLog);
            if(result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())){
                CastException.cast(ShopCode.SHOP_USER_MONEY_REDUCE_FAIL);
            }
            logger.info("订单："+order.getOrderId()+",扣减余额成功");
        }
    }

    /**
     * 扣减优惠卷
     * @param order
     */
    private void updateCouponStatus(TradeOrder order) {
        if(order.getCouponId()!=null){
            TradeCoupon coupon = couponService.findOne(order.getCouponId());
            coupon.setOrderId(order.getOrderId());
            coupon.setUsedTime(new Date());
            coupon.setIsUsed(ShopCode.SHOP_COUPON_ISUSED.getCode());

            //更新优惠卷状态
            Result result=couponService.updateCouponStatus(coupon);
            if(result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())){
                CastException.cast(ShopCode.SHOP_ORDER_CONFIRM_FAIL);
            }
            logger.info("订单："+order.getOrderId()+"，使用优惠卷");
        }
    }

    /**
     * 扣减库存
     * @param order
     */
    private void reduceGoodsNum(TradeOrder order) {
        TradeGoodsNumberLog tradeGoodsNumberLog = new TradeGoodsNumberLog();
        tradeGoodsNumberLog.setOrderId(order.getOrderId());
        tradeGoodsNumberLog.setGoodsNumber(order.getGoodsNumber());
        tradeGoodsNumberLog.setGoodsId(order.getGoodsId());
        tradeGoodsNumberLog.setLogTime(new Date());
        Result result=goodsService.reduceGoodsNum(tradeGoodsNumberLog);
        if(!result.getSuccess()){
            CastException.cast(ShopCode.SHOP_REDUCE_GOODS_NUM_FAIL);
        }
        logger.info("订单:"+order.getOrderId()+"扣减库存成功");
    }

    /**
     * 校验订单
     * @param order
     */
    private void checkOrder(TradeOrder order) {
        logger.info("校验订单开始");
        //1.校验订单是否存在
        if(order==null){
            CastException.cast(ShopCode.SHOP_ORDER_INVALID);
        }
        //2.校验订单中的商品是否存在
        TradeGoods tradeGoods = goodsService.findOne(order.getGoodsId());
        if(tradeGoods==null){
            CastException.cast(ShopCode.SHOP_ORDER_INVALID);
        }
        //3.校验下单用户是否存在
        TradeUser tradeUser=userService.findOne(order.getUserId());
        if(tradeUser==null){
            CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
        }
        //4.校验商品单价是否合法
        if(order.getGoodsPrice().compareTo(tradeGoods.getGoodsPrice())!=0){
            CastException.cast(ShopCode.SHOP_GOODS_PRICE_INVALID);
        }
        //5.校验订单商品数量是否合法
        if(order.getGoodsNumber()>tradeGoods.getGoodsNumber()){
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }
        logger.info("校验订单通过");
    }

    /**
     * 生成预订单
     * @param order
     * @return
     */
    private Long savePreOrder(TradeOrder order){
        //1.设置订单状态不可见
        order.setOrderStatus(ShopCode.SHOP_ORDER_NO_CONFIRM.getCode());
        //2.设置订单ID
        order.setOrderId(idWorker.nextId());
        //3.核算订单运费
        BigDecimal shippingFee=calculateShippingFee(order.getOrderAmount());
        if(shippingFee.compareTo(order.getShippingFee())!=0){
            CastException.cast(ShopCode.SHOP_ORDER_SHIPPINGFEE_INVALID);
        }
        //4.核算订单总金额是否合法
        BigDecimal orderAmount = order.getGoodsPrice().multiply(new BigDecimal(order.getGoodsNumber()));
        orderAmount.add(shippingFee);
        if(orderAmount.compareTo(order.getOrderAmount())!=0){
            CastException.cast(ShopCode.SHOP_ORDERAMOUNT_INVALID);
        }
        //5.判断用户是否使用余额
        BigDecimal moneyPaid = order.getMoneyPaid();
        if(moneyPaid!=null){
            int i = moneyPaid.compareTo(BigDecimal.ZERO);
            if(i==1){
                TradeUser user = userService.findOne(order.getUserId());
                if(moneyPaid.compareTo(user.getUserMoney())==1){
                    CastException.cast(ShopCode.SHOP_MONEY_PAID_INVALID);
                }
            }
            if(i==-1){
                CastException.cast(ShopCode.SHOP_MONEY_PAID_LESS_ZERO);
            }
        }else{
            order.setMoneyPaid(BigDecimal.ZERO);
        }
        //6.判断用户是否使用优惠卷
        Long couponId = order.getCouponId();
        if(couponId!=null){
            //6.1 判断优惠卷是否存在
            TradeCoupon tradeCoupon = couponService.findOne(couponId);
            if(tradeCoupon==null){
                CastException.cast(ShopCode.SHOP_COUPON_NO_EXIST);
            }
            //6.2 判断优惠卷是否已经被使用
            Integer isUsed = tradeCoupon.getIsUsed();
            if(isUsed==1){
                CastException.cast(ShopCode.SHOP_COUPON_ISUSED);
            }
            order.setCouponPaid(tradeCoupon.getCouponPrice());
        }else{
            order.setCouponPaid(BigDecimal.ZERO);
        }
        //7.核算订单支付金额
        BigDecimal payAmount = order.getOrderAmount().subtract(order.getMoneyPaid()).subtract(order.getCouponPaid());
        order.setPayAmount(payAmount);
        //8.设置下单时间
        order.setAddTime(new Date());
        //9.保存订单到s数据库
        tradeOrderMapper.insert(order);
        //10.返回订单ID
        return order.getOrderId();
    }

    /**
     * 核算运费
     * @param orderAmount
     * @return
     */
    private BigDecimal calculateShippingFee(BigDecimal orderAmount) {
        if(orderAmount.compareTo(new BigDecimal(100))==1){
            return BigDecimal.ZERO;
        }else{
            return new BigDecimal(10);
        }
    }
}
