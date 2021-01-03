package z.hong.shoporderweb;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import z.hong.shopcommon.utils.IDWorker;

@EnableScheduling
@EnableDubboConfiguration
@SpringBootApplication
public class ShopOrderWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopOrderWebApplication.class, args);
    }

    @Bean
    public IDWorker getIDWorker(){
        return new IDWorker(1,2);
    }
}
