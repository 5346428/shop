package z.hong.shopapi.api;

import z.hong.shoppojo.entity.Result;
import z.hong.shoppojo.pojo.TradeCoupon;

public interface ICouponService {

    /**
     * 根据id进行查询优惠卷
     * @param couponId
     * @return
     */
    TradeCoupon findOne(Long couponId);

    /**
     * 更新优惠卷
     * @param coupon
     * @return
     */
    Result updateCouponStatus(TradeCoupon coupon);
}
