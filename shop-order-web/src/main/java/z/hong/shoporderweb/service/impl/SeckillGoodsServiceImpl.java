package z.hong.shoporderweb.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import z.hong.shopcommon.utils.IDWorker;
import z.hong.shoporderweb.mapper.TbSeckillGoodsMapper;
import z.hong.shoporderweb.service.SeckillGoodsService;
import z.hong.shoporderweb.thread.OrderCreateThread;
import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.SeckillOrderRecord;
import z.hong.shoppojo.pojo.TbSeckillGoods;
import z.hong.shoppojo.pojo.TbSeckillOrder;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderCreateThread orderCreateThread;
    private  ExecutorService executorService = Executors.newCachedThreadPool();
    private Logger log= LoggerFactory.getLogger(SeckillGoodsServiceImpl.class);

    @Override
    public List<TbSeckillGoods> findAll() {
        log.info("查询全部秒杀杀商品");
        return redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).values();
    }

    @Override
    public TbSeckillGoods findOne(Long id) {
        return (TbSeckillGoods)redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(id.toString());
    }

    @Override
    public Result saveOrder(Long id, String userId) {

         //出现多线程超卖问题
//        //1.从redis获取秒杀商品
//        TbSeckillGoods tbSeckillGoods = (TbSeckillGoods)redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(id.toString());
//        //2.判断秒杀商品是否存在，或库存s是否<=0
//        if(tbSeckillGoods==null||tbSeckillGoods.getStockCount()<=0){
//            //3.商品不存在或库存<=0，返回失败，提示已卖完
//            return new Result(false,"该商品已卖完");
//        }
        //0.防止用户多次购买
        Boolean member = redisTemplate.boundSetOps("CONST_USER_ID_PRRFIX" + id).isMember(userId);
        if(member==true){
            return new Result(false,"你已购买，请尽快支付");
        }
        //解决多线程问题
        //1.从队列里获取秒杀商品
        String qId =(String)redisTemplate.boundListOps("CONST_SECKILLGOODS_ID_PRRFIX" + id).rightPop();
        //2.判断秒杀商品是否存在,null则不存在
        if(qId==null){
            //3.返回失败，提示已卖完
            return new Result(false,"该商品已卖完");
        }
        //4.将用户的id保存到集合里面
        redisTemplate.boundSetOps("CONST_USER_ID_PRRFIX" + id).add(userId);
        //5.将SeckillOrderRecord对象保存到list
        SeckillOrderRecord seckillOrderRecord = new SeckillOrderRecord(id,userId);
        redisTemplate.boundListOps(SeckillOrderRecord.class.getSimpleName()).leftPush(seckillOrderRecord);
        executorService.execute(orderCreateThread);
        return new Result(true,"秒杀成功，请你尽快支付");
    }
}
