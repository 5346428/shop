package z.hong.shoporderweb.service;

import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TbSeckillGoods;

import java.util.List;

public interface SeckillGoodsService {
    List<TbSeckillGoods> findAll();
    TbSeckillGoods findOne(Long id);

    Result saveOrder(Long id, String userId);
}
