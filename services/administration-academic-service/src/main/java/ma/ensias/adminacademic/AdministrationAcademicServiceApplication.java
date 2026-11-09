package ma.ensias.adminacademic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AdministrationAcademicServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdministrationAcademicServiceApplication.class, args);
    }
}