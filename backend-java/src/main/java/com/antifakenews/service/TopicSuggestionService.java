package com.antifakenews.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sugiere temas para una noticia a partir de su texto (título, contenido, URL,
 * fuente) usando reglas simples por palabra clave. No depende de servicios
 * externos ni de IA.
 *
 * Prioriza los temas provistos por el frontend/IA (marcados USER_OR_AI) y luego
 * agrega los detectados automáticamente (AUTO). Si no se detecta ninguno, asocia
 * "General" para que toda noticia tenga al menos un tema.
 */
@Service
public class TopicSuggestionService {

    public static final String SOURCE_USER_OR_AI = "USER_OR_AI";
    public static final String SOURCE_AUTO = "AUTO";
    private static final String FALLBACK_TOPIC = "General";

    /** Tema -> palabras clave (en minúsculas y sin acentos) que lo disparan. */
    private static final Map<String, List<String>> RULES = Map.of(
            "Salud", List.of("salud", "vacuna", "dengue", "medicamento", "cura", "hospital"),
            "Tecnología", List.of("5g", "tecnologia", "inteligencia artificial", "redes", "internet"),
            "Economía", List.of("dolar", "economia", "banco", "mercado", "inflacion", "colapso economico"),
            "Política", List.of("elecciones", "gobierno", "presidente", "candidato", "congreso"),
            "Clima", List.of("clima", "lluvia", "temperatura", "calentamiento", "ambiental"),
            "Seguridad", List.of("seguridad", "policia", "delito", "robo")
    );

    /**
     * Devuelve la lista de temas a asociar, cada uno con su origen ("name" y "source").
     * Lista apta para pasar como parámetro de Cypher (List of Map).
     */
    public List<Map<String, Object>> suggest(String title, String content, String url,
                                              String sourceName, List<String> provided) {
        // Mapa ordenado nombre -> source, para deduplicar respetando prioridad.
        Map<String, String> topics = new LinkedHashMap<>();

        if (provided != null) {
            for (String name : provided) {
                if (name != null && !name.isBlank()) {
                    topics.putIfAbsent(name.trim(), SOURCE_USER_OR_AI);
                }
            }
        }

        String haystack = normalize(String.join(" ",
                safe(title), safe(content), safe(url), safe(sourceName)));
        for (Map.Entry<String, List<String>> rule : RULES.entrySet()) {
            if (containsAny(haystack, rule.getValue())) {
                topics.putIfAbsent(rule.getKey(), SOURCE_AUTO);
            }
        }

        if (topics.isEmpty()) {
            topics.put(FALLBACK_TOPIC, SOURCE_AUTO);
        }

        List<Map<String, Object>> result = new ArrayList<>(topics.size());
        for (Map.Entry<String, String> entry : topics.entrySet()) {
            result.add(Map.of("name", entry.getKey(), "source", entry.getValue()));
        }
        return result;
    }

    private boolean containsAny(String haystack, List<String> keywords) {
        for (String keyword : keywords) {
            if (haystack.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /** Minúsculas y sin diacríticos, para que "tecnología" matchee "tecnologia". */
    private String normalize(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
