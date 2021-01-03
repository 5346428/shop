package z.hong.shoporderservice;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import z.hong.shopcommon.utils.IDWorker;

//@MapperScan("z.hong.shoporderservice.mapper")
@EnableDubboConfiguration
@SpringBootApplication
public class ShopOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopOrderServiceApplication.class, args);
    }

    @Bean
    public IDWorker getIDWorker(){
        return new IDWorker(1,1);
    }
}
