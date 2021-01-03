package z.hong.shopcouponservice;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


//@MapperScan("z.hong.shopcouponservice.mapper")
@EnableDubboConfiguration
@SpringBootApplication
public class ShopCouponServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopCouponServiceApplication.class, args);
    }

}
