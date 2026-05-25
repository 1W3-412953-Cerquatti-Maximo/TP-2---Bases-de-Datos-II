package com.antifakenews.init;

import com.antifakenews.repository.AuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Inicializador demo opcional (activado con demo.seed.enabled=true, default true).
 *
 * Al arrancar:
 *  - crea o actualiza la cuenta demo (password hasheado con BCrypt),
 *  - vincula todas las noticias sin dueño a la cuenta demo vía OWNS_NEWS.
 *
 * Idempotente: re-ejecutar no duplica usuarios ni relaciones. Si Neo4j no está
 * disponible, se registra una advertencia y la app arranca igual.
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private static final String DEMO_USERNAME = "demo";
    private static final String DEMO_EMAIL = "demo@nexoveraz.local";
    private static final String DEMO_DISPLAY_NAME = "Demo NexoVeraz";
    private static final String DEMO_PASSWORD = "123456";
    private static final String DEMO_ROLE = "USER";
    private static final String DEMO_THEME = "dark";

    @Value("${demo.seed.enabled:true}")
    private boolean enabled;

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(AuthRepository authRepository, PasswordEncoder passwordEncoder) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Demo seed deshabilitado (demo.seed.enabled=false).");
            return;
        }
        try {
            String passwordHash = passwordEncoder.encode(DEMO_PASSWORD);
            String demoId = authRepository.upsertUser(
                    UUID.randomUUID().toString(),
                    DEMO_USERNAME, DEMO_EMAIL, DEMO_DISPLAY_NAME,
                    passwordHash, DEMO_ROLE, DEMO_THEME);

            long linked = authRepository.linkOwnerlessNewsTo(demoId);
            log.info("Demo seed OK: cuenta demo lista (id={}), {} noticias sin dueño vinculadas.", demoId, linked);
        } catch (Exception e) {
            log.warn("Demo seed omitido (¿Neo4j disponible y constraints cargados?): {}", e.getMessage());
        }
    }
}
