package z.hong.shopgoodsservice.mq;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import z.hong.shopcommon.constant.ShopCode;
import z.hong.shopgoodsservice.mapper.TradeGoodsMapper;
import z.hong.shopgoodsservice.mapper.TradeGoodsNumberLogMapper;
import z.hong.shopgoodsservice.mapper.TradeMqConsumerLogMapper;
import z.hong.shoppojo.entity.MQEntity;
import z.hong.shoppojo.pojo.*;

import java.io.UnsupportedEncodingException;
import java.util.Date;

@Component
@RocketMQMessageListener(topic = "${mq.order.topic}",consumerGroup = "${mq.order.consumer.group.name}",messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Value("${mq.order.consumer.group.name}")
    private String groupName;
    @Autowired
    private TradeMqConsumerLogMapper mqConsumerLogMapper;
    @Autowired
    private TradeGoodsMapper goodsMapper;
    @Autowired
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;
    private Logger logger= LoggerFactory.getLogger(CancelMQListener.class);

    @Override
    public void onMessage(MessageExt messageExt) {
        String msgId =null;
        String tags = null;
        String keys = null;
        String body = null;
        try {
            //1.解析消息内容
            msgId = messageExt.getMsgId();
            tags = messageExt.getTags();
            keys = messageExt.getKeys();
            body = new String(messageExt.getBody(), "UTF-8");
            logger.info("接受回退库存消息成功");
            //2.查询消息消费记录
            TradeMqConsumerLogKey primary=new TradeMqConsumerLogKey();
            primary.setMsgKey(keys);
            primary.setMsgTag(tags);
            primary.setGroupName(groupName);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(primary);
            if(mqConsumerLog!=null){
                //3.判断如果消费过
                //3.1获得消息的处理状态
                Integer status = mqConsumerLog.getConsumerStatus();
                //处理过...返回
                if(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode()==status){
                    logger.info("消息："+msgId+",已经处理过");
                    return;
                }
                //正在处理...返回
                if(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode()==status){
                    logger.info("消息："+msgId+",正在处理");
                    return;
                }
                //处理失败
                if(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode()==status){
                    //获得消息处理的次数
                    Integer times = mqConsumerLog.getConsumerTimes();
                    if(times>3){
                        logger.info("消息："+msgId+",消息处理超过3次");
                        return;
                    }
                    mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
                    TradeMqConsumerLogExample example=new TradeMqConsumerLogExample();
                    TradeMqConsumerLogExample.Criteria criteria = example.createCriteria();
                    criteria.andMsgTagEqualTo(tags);
                    criteria.andMsgKeyEqualTo(keys);
                    criteria.andGroupNameEqualTo(groupName);
                    criteria.andConsumerTimesEqualTo(times);
                    //数据库乐观锁防止并发问题
                    int r = mqConsumerLogMapper.updateByExampleSelective(mqConsumerLog, example);
                    if(r<=0){
                        //未修改成功,其他线程并发x修改
                        logger.info("并发修改失败，请稍后处理");
                    }
                }
            }else{
                //4.判断如果没有消费过
                //4. 判断如果没有消费过...
                mqConsumerLog = new TradeMqConsumerLog();
                mqConsumerLog.setMsgTag(tags);
                mqConsumerLog.setMsgKey(keys);
                mqConsumerLog.setGroupName(groupName);
                mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
                mqConsumerLog.setMsgBody(body);
                mqConsumerLog.setMsgId(msgId);
                mqConsumerLog.setConsumerTimes(0);

                //将消息处理信息添加到数据库
                mqConsumerLogMapper.insert(mqConsumerLog);
            }
            //5.回退库存
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            Long goodsId = mqEntity.getGoodsId();
            TradeGoods goods = goodsMapper.selectByPrimaryKey(goodsId);
            goods.setGoodsNumber(goods.getGoodsNumber()+mqEntity.getGoodsNum());
            goodsMapper.updateByPrimaryKey(goods);


            //6. 将消息的处理状态改为成功
            mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode());
            mqConsumerLog.setConsumerTimestamp(new Date());
            mqConsumerLogMapper.updateByPrimaryKey(mqConsumerLog);
            logger.info("回退库存成功");

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            TradeMqConsumerLogKey primaryKey = new TradeMqConsumerLogKey();
            primaryKey.setMsgTag(tags);
            primaryKey.setMsgKey(keys);
            primaryKey.setGroupName(groupName);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(primaryKey);
            if(mqConsumerLog==null){
                //数据库未有记录
                mqConsumerLog = new TradeMqConsumerLog();
                mqConsumerLog.setMsgTag(tags);
                mqConsumerLog.setMsgKey(keys);
                mqConsumerLog.setGroupName(groupName);
                mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode());
                mqConsumerLog.setMsgBody(body);
                mqConsumerLog.setMsgId(msgId);
                mqConsumerLog.setConsumerTimes(1);
                mqConsumerLogMapper.insert(mqConsumerLog);
            }else{
                mqConsumerLog.setConsumerTimes(mqConsumerLog.getConsumerTimes()+1);
                mqConsumerLogMapper.updateByPrimaryKeySelective(mqConsumerLog);
            }
        }
    }
}
