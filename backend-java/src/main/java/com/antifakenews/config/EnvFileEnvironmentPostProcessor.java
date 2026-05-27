package com.antifakenews.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga claves locales desde un archivo de secretos en la raíz del proyecto
 * (.env.API_KEY) para desarrollo local. Spring Boot no lee este archivo custom
 * automáticamente, así que lo hacemos al iniciar.
 *
 * Reglas de seguridad:
 * - Las variables se agregan con la MENOR prioridad (addLast): si existe la
 *   misma variable como variable de entorno real, ESA tiene prioridad.
 * - NUNCA se loguean los valores (solo el nombre del archivo y la cantidad).
 * - Si el archivo no existe, no pasa nada (la app arranca igual).
 *
 * Registrado en:
 * META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports
 */
public class EnvFileEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "nexoVerazSecretsFile";
    private static final List<String> CANDIDATE_NAMES = List.of(".env.API_KEY", ".enc.API_KEY");
    private static final List<String> CANDIDATE_DIRS = List.of("", "..", "../..");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path file = locate();
        if (file == null) {
            return;
        }

        Map<String, Object> values = parse(file);
        if (values.isEmpty()) {
            return;
        }

        // addLast => menor prioridad: las variables de entorno reales ganan.
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, values));
        System.out.println("[NexoVeraz] Cargada(s) " + values.size()
                + " variable(s) desde " + file.getFileName()
                + " (los valores no se muestran por seguridad).");
    }

    private Path locate() {
        for (String dir : CANDIDATE_DIRS) {
            for (String name : CANDIDATE_NAMES) {
                Path candidate = dir.isEmpty() ? Path.of(name) : Path.of(dir, name);
                if (Files.isRegularFile(candidate)) {
                    return candidate.toAbsolutePath().normalize();
                }
            }
        }
        return null;
    }

    private Map<String, Object> parse(Path file) {
        Map<String, Object> map = new HashMap<>();
        try {
            for (String raw : Files.readAllLines(file)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                value = stripQuotes(value);
                if (!key.isEmpty()) {
                    map.put(key, value);
                }
            }
        } catch (IOException e) {
            // No exponemos contenido del archivo; solo señalamos que no se pudo leer.
            System.out.println("[NexoVeraz] No se pudo leer " + file.getFileName() + ": " + e.getMessage());
        }
        return map;
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
