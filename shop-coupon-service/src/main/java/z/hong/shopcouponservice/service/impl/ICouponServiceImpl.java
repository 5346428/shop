package z.hong.shopcouponservice.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import z.hong.shopapi.api.ICouponService;
import z.hong.shopcommon.constant.ShopCode;
import z.hong.shopcommon.exception.CastException;
import z.hong.shopcouponservice.mapper.TradeCouponMapper;
import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TradeCoupon;

@Component
@Service(interfaceClass = ICouponService.class)
public class ICouponServiceImpl implements ICouponService {

    @Autowired
    TradeCouponMapper couponMapper;

    /**
     * 根据id进行查询优惠卷
     * @param couponId
     * @return
     */
    @Override
    public TradeCoupon findOne(Long couponId) {
        if(couponId==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return couponMapper.selectByPrimaryKey(couponId);
    }

    /**
     * 更新优惠卷
     * @param coupon
     * @return
     */
    @Override
    public Result updateCouponStatus(TradeCoupon coupon) {
        if(coupon==null||coupon.getCouponId()==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //更新优惠卷状态
        couponMapper.updateByPrimaryKey(coupon);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
