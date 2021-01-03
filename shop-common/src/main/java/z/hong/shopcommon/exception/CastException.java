package z.hong.shopcommon.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import z.hong.shopcommon.constant.ShopCode;

/**
 * 异常抛出类
 */
public class CastException {
    private static Logger logger= LoggerFactory.getLogger(CastException.class);
    public static void cast(ShopCode shopCode) {
        logger.error(shopCode.toString());
        throw new CustomerException(shopCode);
    }
}

