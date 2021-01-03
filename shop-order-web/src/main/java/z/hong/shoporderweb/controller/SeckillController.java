package z.hong.shoporderweb.controller;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import z.hong.shoporderweb.service.SeckillGoodsService;
import z.hong.shoporderweb.service.impl.SeckillGoodsServiceImpl;
import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TbSeckillGoods;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/seckillGoods")
public class SeckillController {

    @Autowired
    private SeckillGoodsService goodsService;
    private Logger log= LoggerFactory.getLogger(SeckillController.class);

    @RequestMapping("/findAll")
    public List<TbSeckillGoods> findAll(){
        log.info("正在调用查询全部秒杀商品接口");
        return goodsService.findAll();
    }
    @RequestMapping(value = "/findOne")
    public TbSeckillGoods findOne(@RequestParam("id") Long id){
        log.info("正在调用查询秒杀商品接口");
        return goodsService.findOne(id);
    }
    @RequestMapping(value = "/saveOrder")
    public Result saveOrder(@RequestParam("id") Long id){
        log.info("正在调用保存订单的接口");
        String userId="zhangsan"+new Date();
        return goodsService.saveOrder(id,userId);
    }
}
