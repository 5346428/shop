package z.hong.shoppayservice;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import z.hong.shopcommon.utils.IDWorker;

@EnableDubboConfiguration
@SpringBootApplication
public class ShopPayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopPayServiceApplication.class, args);
    }

    @Bean
    public IDWorker getIDWorker(){
        return new IDWorker(1,2);
    }
}
