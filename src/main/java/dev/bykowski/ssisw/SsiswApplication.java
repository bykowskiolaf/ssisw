package dev.bykowski.ssisw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SsiswApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(SsiswApplication.class);
        // JADE's optional RMA window needs AWT enabled before Boot configures the JVM.
        application.setHeadless(false);
        application.run(args);
    }

}
