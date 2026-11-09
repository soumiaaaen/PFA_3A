package ma.ensias.adminacademic.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.ensias.adminacademic.identity.entity.RoleCode;
import ma.ensias.adminacademic.identity.repository.UserRepository;
import ma.ensias.adminacademic.identity.web.dto.CreateUserRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.bootstrap.admin-email}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByRoles_Code(RoleCode.ADMIN)) {
            userService.create(new CreateUserRequest(
                    adminEmail, adminPassword, "Admin", "ENSIAS", Set.of(RoleCode.ADMIN)));
            log.info("Compte administrateur initial créé : {}", adminEmail);
        }
    }
}