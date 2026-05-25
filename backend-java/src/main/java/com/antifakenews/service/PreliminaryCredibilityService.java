package com.antifakenews.service;

import com.antifakenews.dto.CredibilityDiagnosisDto;
import com.antifakenews.dto.RiskSignalDto;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PreliminaryCredibilityService {

    private static final int MEDIUM_LEVEL_MIN = 40;
    private static final int HIGH_LEVEL_MIN = 70;
    private static final int MAX_SCORE = 100;

    private static final int VERY_SHORT_CONTENT_THRESHOLD = 400;
    private static final int SHORT_CONTENT_THRESHOLD = 900;

    private static final List<String> SUSPICIOUS_TLDS = List.of(
            ".xyz", ".top", ".click", ".buzz", ".gq", ".tk", ".ml"
    );

    private static final List<String> ALARMIST_TERMS = List.of(
            "urgente", "escandalo", "escándalo", "impactante", "bomba", "colapso",
            "devastador", "explosivo", "increible", "increíble", "ultima hora", "última hora"
    );

    private static final List<String> EXTRAORDINARY_TERMS = List.of(
            "cura milagrosa", "secreto que no quieren que sepas", "100% garantizado",
            "confirmado sin pruebas", "conspiracion", "conspiración", "milagro", "imposible"
    );

    private static final Map<String, String> REASON_PHRASES = new LinkedHashMap<>();

    static {
        REASON_PHRASES.put("SUSPICIOUS_DOMAIN", "el dominio presenta patrones típicos de sitios poco confiables");
        REASON_PHRASES.put("SCARCE_ARTICLE_TEXT", "el artículo ofrece poco texto recuperable para validar contexto y fuentes");
        REASON_PHRASES.put("MISSING_PUBLICATION_DATE", "no expone una fecha de publicación clara en la metadata");
        REASON_PHRASES.put("LIMITED_EDITORIAL_METADATA", "no muestra metadata editorial suficiente del medio");
        REASON_PHRASES.put("SENSATIONALIST_TITLE", "el titular usa recursos sensacionalistas");
        REASON_PHRASES.put("ALARMIST_LANGUAGE", "el cuerpo del texto usa lenguaje alarmista o de urgencia");
        REASON_PHRASES.put("EXTRAORDINARY_CLAIMS", "aparecen afirmaciones extraordinarias que requieren evidencia fuerte");
    }

    public CredibilityDiagnosisDto diagnose(WebArticleExtractionService.ExtractedArticle article) {
        List<RiskSignalDto> signals = new ArrayList<>();
        int rawScore = 0;

        if (looksSuspiciousDomain(article.resolvedUrl())) {
            signals.add(new RiskSignalDto(
                    "SUSPICIOUS_DOMAIN",
                    "El dominio resuelto usa un TLD o un patrón de hostname frecuente en sitios de baja reputación.",
                    18));
            rawScore += 18;
        }

        if (article.content().length() < VERY_SHORT_CONTENT_THRESHOLD) {
            signals.add(new RiskSignalDto(
                    "SCARCE_ARTICLE_TEXT",
                    "Se recuperó muy poco texto útil del artículo; eso limita la capacidad de contrastar contexto, datos y atribuciones.",
                    28));
            rawScore += 28;
        } else if (article.content().length() < SHORT_CONTENT_THRESHOLD) {
            signals.add(new RiskSignalDto(
                    "SCARCE_ARTICLE_TEXT",
                    "El artículo tiene poco desarrollo textual para una validación robusta.",
                    14));
            rawScore += 14;
        }

        if (!article.hasDateMetadata()) {
            signals.add(new RiskSignalDto(
                    "MISSING_PUBLICATION_DATE",
                    "La página no expone una fecha de publicación clara en su metadata pública.",
                    10));
            rawScore += 10;
        }

        if (!article.hasSourceMetadata()) {
            signals.add(new RiskSignalDto(
                    "LIMITED_EDITORIAL_METADATA",
                    "No se detectó metadata editorial clara del medio y se debió usar el dominio como referencia principal.",
                    10));
            rawScore += 10;
        }

        if (isSensationalistTitle(article.title())) {
            signals.add(new RiskSignalDto(
                    "SENSATIONALIST_TITLE",
                    "El titular contiene marcas típicas de sensacionalismo, urgencia o exageración.",
                    20));
            rawScore += 20;
        }

        if (containsAlarmistLanguage(article.content())) {
            signals.add(new RiskSignalDto(
                    "ALARMIST_LANGUAGE",
                    "El texto usa un tono alarmista o de cadena viral que conviene contrastar con fuentes adicionales.",
                    16));
            rawScore += 16;
        }

        if (containsExtraordinaryClaims(article.title(), article.content())) {
            signals.add(new RiskSignalDto(
                    "EXTRAORDINARY_CLAIMS",
                    "Se detectaron expresiones asociadas a afirmaciones extraordinarias o poco verificables.",
                    24));
            rawScore += 24;
        }

        int riskScore = Math.min(rawScore, MAX_SCORE);
        String riskLevel = computeLevel(riskScore);

        return new CredibilityDiagnosisDto(
                riskScore,
                riskLevel,
                buildSummary(riskLevel, signals),
                List.copyOf(signals),
                true,
                "Diagnóstico preliminar basado en texto extraído y metadata pública del enlace. No usa el grafo Neo4j ni persiste la noticia."
        );
    }

    private boolean looksSuspiciousDomain(String resolvedUrl) {
        String host = extractHost(resolvedUrl).toLowerCase(Locale.ROOT);
        if (host.isBlank()) return false;

        for (String tld : SUSPICIOUS_TLDS) {
            if (host.endsWith(tld)) {
                return true;
            }
        }

        long hyphenCount = host.chars().filter(ch -> ch == '-').count();
        long digitCount = host.chars().filter(Character::isDigit).count();
        return hyphenCount >= 3 || digitCount >= 4;
    }

    private boolean isSensationalistTitle(String title) {
        String normalized = normalize(title);
        if (normalized.isBlank()) return false;

        if (title.chars().filter(ch -> ch == '!').count() >= 2) {
            return true;
        }

        return ALARMIST_TERMS.stream().anyMatch(normalized::contains) || hasAllCapsWord(title);
    }

    private boolean containsAlarmistLanguage(String content) {
        String normalized = normalize(content);
        if (normalized.isBlank()) return false;

        long matches = ALARMIST_TERMS.stream().filter(normalized::contains).count();
        return matches >= 2 || normalized.contains("comparti") || normalized.contains("viral");
    }

    private boolean containsExtraordinaryClaims(String title, String content) {
        String normalized = normalize(title + " " + content);
        return EXTRAORDINARY_TERMS.stream().anyMatch(normalized::contains);
    }

    private boolean hasAllCapsWord(String title) {
        for (String token : title.split("\\s+")) {
            String letters = token.replaceAll("[^A-Za-zÁÉÍÓÚÑÜáéíóúñü]", "");
            if (letters.length() >= 5 && letters.equals(letters.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String computeLevel(int score) {
        if (score >= HIGH_LEVEL_MIN) return "HIGH";
        if (score >= MEDIUM_LEVEL_MIN) return "MEDIUM";
        return "LOW";
    }

    private String buildSummary(String level, List<RiskSignalDto> signals) {
        String levelText = switch (level) {
            case "HIGH" -> "credibilidad frágil";
            case "MEDIUM" -> "credibilidad en revisión";
            default -> "credibilidad razonable, pero preliminar";
        };

        if (signals.isEmpty()) {
            return "Diagnóstico preliminar de credibilidad: el enlace muestra " + levelText
                    + " y no activó señales textuales o de metadata suficientes para elevar el riesgo. Conviene igual contrastarlo con otras fuentes.";
        }

        List<String> reasons = signals.stream()
                .map(signal -> REASON_PHRASES.getOrDefault(signal.code(), signal.code()))
                .toList();

        String joinedReasons;
        if (reasons.size() == 1) {
            joinedReasons = reasons.get(0);
        } else {
            String tail = reasons.get(reasons.size() - 1);
            String head = String.join(", ", reasons.subList(0, reasons.size() - 1));
            joinedReasons = head + " y " + tail;
        }

        return "Diagnóstico preliminar de credibilidad: el enlace sugiere " + levelText
                + " porque " + joinedReasons + ".";
    }

    private String extractHost(String resolvedUrl) {
        try {
            URI uri = new URI(resolvedUrl);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (URISyntaxException ex) {
            return "";
        }
    }
}
