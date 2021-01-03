package z.hong.shoporderweb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import z.hong.shoporderweb.mapper.TbSeckillGoodsMapper;
import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TbSeckillGoods;
import z.hong.shoppojo.pojo.TbSeckillGoodsExample;
import z.hong.shoppojo.pojo.TradeOrder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ShopOrderWebApplication.class)
public class OrderWebTest {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${shop.order.baseURI}")
    private String baseURI;
    @Value("${shop.order.confirm}")
    private String confirmOrderPath;

    @Test
    public void confirmOrder() throws IOException {
        Long coupouId = 345988230098857984L;
        Long goodsId = 345959443973935104L;
        Long userId = 345963634385633280L;

        TradeOrder order = new TradeOrder();
        order.setGoodsId(goodsId);
        order.setUserId(userId);
        order.setCouponId(coupouId);
        order.setAddress("北京");
        order.setGoodsNumber(1);
        order.setGoodsPrice(new BigDecimal(1000));
        order.setShippingFee(BigDecimal.ZERO);
        order.setOrderAmount(new BigDecimal(1000));
        order.setMoneyPaid(new BigDecimal(100));

        Result result = restTemplate.postForEntity(baseURI + confirmOrderPath, order, Result.class).getBody();

        System.out.println(result);
    }
    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    @Scheduled(cron = "0/30 1 12 * * ? ")
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
