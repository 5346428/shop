package z.hong.shopcouponservice.mq;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import z.hong.shopcommon.constant.ShopCode;
import z.hong.shopcouponservice.mapper.TradeCouponMapper;
import z.hong.shoppojo.entity.MQEntity;
import z.hong.shoppojo.pojo.TradeCoupon;

import java.io.UnsupportedEncodingException;

@Component
@RocketMQMessageListener(topic = "${mq.order.topic}",consumerGroup = "${mq.order.consumer.group.name}",messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeCouponMapper couponMapper;
    private Logger logger= LoggerFactory.getLogger(CancelMQListener.class);

    @Override
    public void onMessage(MessageExt messageExt) {
        try {
            //1. 解析消息内容
            String body = new String(messageExt.getBody(), "UTF-8");
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            logger.info("接收到消息");
            if(mqEntity.getCouponId()!=null){
                //2. 查询优惠券信息
                TradeCoupon coupon = couponMapper.selectByPrimaryKey(mqEntity.getCouponId());
                //3.更改优惠券状态
                coupon.setUsedTime(null);
                coupon.setIsUsed(ShopCode.SHOP_COUPON_UNUSED.getCode());
                coupon.setOrderId(null);
                couponMapper.updateByPrimaryKey(coupon);
            }
            logger.info("回退优惠券成功");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error("回退优惠券失败");
        }

    }
}
