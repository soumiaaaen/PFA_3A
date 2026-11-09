package ma.ensias.adminacademic;

import org.springframework.boot.SpringApplication;

public class TestAdministrationAcademicServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(AdministrationAcademicServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
