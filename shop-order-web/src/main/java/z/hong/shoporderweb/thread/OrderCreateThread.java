package z.hong.shoporderweb.thread;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import z.hong.shopcommon.utils.IDWorker;
import z.hong.shoporderweb.mapper.TbSeckillGoodsMapper;
import z.hong.shoppojo.pojo.SeckillOrderRecord;
import z.hong.shoppojo.pojo.TbSeckillGoods;
import z.hong.shoppojo.pojo.TbSeckillOrder;

import javax.annotation.Resource;
import java.util.Date;

@Component
public class OrderCreateThread implements Runnable{

    @Resource
    private IDWorker idWorker;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private TbSeckillGoodsMapper goodsMapper;

    @Override
    public void run() {
        SeckillOrderRecord orderRecord =(SeckillOrderRecord)redisTemplate.boundListOps(SeckillOrderRecord.class.getSimpleName()).rightPop();
        //4.从redis获取秒杀商品
        TbSeckillGoods tbSeckillGoods = (TbSeckillGoods)redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(orderRecord.getId().toString());
        //5.生成订单，将订单保存到redis
        TbSeckillOrder order = new TbSeckillOrder();
        order.setCreateTime(new Date());
        order.setMoney(tbSeckillGoods.getCostPrice());
        order.setSeckillId(idWorker.nextId());
        order.setUserId(orderRecord.getUserId());
        order.setSellerId(tbSeckillGoods.getSellerId());
        order.setStatus("0");//未支付
        redisTemplate.boundHashOps(TbSeckillOrder.class.getSimpleName()).put(orderRecord.getUserId(),order);

        synchronized (OrderCreateThread.class){
            tbSeckillGoods = (TbSeckillGoods)redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(orderRecord.getId().toString());
            //6.秒杀商品库存量-1
            tbSeckillGoods.setStockCount(tbSeckillGoods.getStockCount()-1);
            //7.判断库存量是否<=0
            if(tbSeckillGoods.getStockCount()<=0){
                //8.是，将秒杀商品更新到数据库，删除秒杀商品缓存
                goodsMapper.updateByPrimaryKey(tbSeckillGoods);
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).delete(orderRecord.getId().toString());
            }else{
                //9.否，将秒杀商品更新到缓存，返回成功。
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(orderRecord.getId().toString(),tbSeckillGoods);
            }
        }
    }
}
