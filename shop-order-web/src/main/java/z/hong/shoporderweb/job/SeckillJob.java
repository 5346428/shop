package z.hong.shoporderweb.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import z.hong.shoporderweb.mapper.TbSeckillGoodsMapper;
import z.hong.shoppojo.pojo.TbSeckillGoods;
import z.hong.shoppojo.pojo.TbSeckillGoodsExample;

import java.util.Date;
import java.util.List;
import java.util.Map;


@Component
public class SeckillJob {

    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Scheduled(cron = "0/30 * 23 * * ? ")
    public void seckillTask(){
        System.out.println("定时任务执行"+new Date());
        //1.查询合法的秒杀商品数据：状态为有效（status=1），库存量>0(stock_count>0),秒杀时间<当前时间<秒杀结束时间
        TbSeckillGoodsExample example=new TbSeckillGoodsExample();
        TbSeckillGoodsExample.Criteria criteria = example.createCriteria();
        Date date=new Date();
        criteria.andStatusEqualTo("1")
                .andStartTimeLessThanOrEqualTo(date).andEndTimeGreaterThan(date);
        List<TbSeckillGoods> tbSeckillGoods = seckillGoodsMapper.selectByExample(example);
        //2.将数据存入redis
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName());
        for(TbSeckillGoods good:tbSeckillGoods){
            boundHashOperations.put(good.getId().toString(),good);
            createQueue(good.getId(),good.getStockCount());
        }
        List<TbSeckillGoods> ob = boundHashOperations.values();
        for(TbSeckillGoods o:ob){
                System.out.println(o);
        }
    }

    private void createQueue(Long id, Integer stockCount) {
        if(stockCount>0){
            for(int i=0;i<stockCount;i++) {
                redisTemplate.boundListOps("CONST_SECKILLGOODS_ID_PRRFIX" + id).leftPush(id.toString());
            }
            List<String> range = redisTemplate.boundListOps("CONST_SECKILLGOODS_ID_PRRFIX" + id).range(0, -1);
            for(String s:range){
                System.out.println(s);
            }
        }

    }
}
