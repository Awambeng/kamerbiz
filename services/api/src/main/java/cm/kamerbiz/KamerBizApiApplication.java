package cm.kamerbiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KamerBizApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(KamerBizApiApplication.class, args);
  }
}
