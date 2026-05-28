package com.antifakenews.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class WebArticleExtractionService {

    private static final int FETCH_TIMEOUT_MS = 8_000;
    private static final int MAX_BODY_SIZE_BYTES = 2_000_000;
    private static final int MAX_CONTENT_CHARS = 20_000;
    private static final int MIN_USABLE_CONTENT_CHARS = 160;
    private static final int SHORT_CONTENT_WARNING_CHARS = 900;
    private static final int PREVIEW_CHARS = 420;
    private static final int MIN_PARAGRAPH_CHARS = 60;
    private static final String USER_AGENT = "NexoVerazBot/1.0 (+https://localhost)";

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(WebArticleExtractionService.class);
    private static final java.util.regex.Pattern JSONLD_DATE =
            java.util.regex.Pattern.compile("\"datePublished\"\\s*:\\s*\"([^\"]+)\"");

    public ExtractedArticle extract(String rawUrl) {
        URI uri = parseAndValidateUrl(rawUrl);
        validatePublicHost(uri.getHost());

        Document document = fetchDocument(uri.toString());
        URI resolvedUri = parseAndValidateUrl(document.location());
        validatePublicHost(resolvedUri.getHost());

        stripBoilerplate(document);

        String title = firstNonBlank(
                metaContent(document, "meta[property=og:title]"),
                metaContent(document, "meta[name=twitter:title]"),
                document.title(),
                resolvedUri.getHost()
        );

        SourceMetadata sourceMetadata = extractSourceMetadata(document, resolvedUri);
        boolean hasDateMetadata = hasDateMetadata(document);
        String content = extractContent(document);

        if (content.length() < MIN_USABLE_CONTENT_CHARS) {
            throw new IllegalArgumentException("No se pudo extraer contenido suficiente del enlace para emitir un diagnóstico.");
        }

        List<String> warnings = new ArrayList<>();
        if (content.length() < SHORT_CONTENT_WARNING_CHARS) {
            warnings.add("Se recuperó poco texto del artículo; el diagnóstico puede ser menos confiable.");
        }
        if (!hasDateMetadata) {
            warnings.add("No se detectó una fecha de publicación clara en la metadata de la página.");
        }
        if (!sourceMetadata.fromMetadata()) {
            warnings.add("No se encontró metadata editorial clara del medio; se usa el dominio como referencia.");
        }

        boolean truncated = content.length() > MAX_CONTENT_CHARS;
        String trimmedContent = truncated ? content.substring(0, MAX_CONTENT_CHARS).trim() : content;
        if (truncated) {
            warnings.add("El contenido fue recortado para no sobrecargar el análisis automático.");
        }

        String fetchStatus = warnings.isEmpty() ? "OK" : "PARTIAL";

        return new ExtractedArticle(
                uri.toString(),
                resolvedUri.toString(),
                title,
                trimmedContent,
                abbreviate(trimmedContent, PREVIEW_CHARS),
                fetchStatus,
                sourceMetadata.name(),
                sourceMetadata.fromMetadata(),
                hasDateMetadata,
                List.copyOf(warnings)
        );
    }

    /**
     * Extracción best-effort de metadata para guardar una noticia por URL.
     * Valida formato y host (puede lanzar IllegalArgumentException ante una URL
     * inválida o privada), pero NUNCA rompe por fallos de red o parsing: si la
     * web bloquea o falla, devuelve un resultado con success=false y los datos
     * que se hayan podido recuperar.
     */
    public BestEffortExtraction extractMetadata(String rawUrl) {
        URI uri = parseAndValidateUrl(rawUrl);
        validatePublicHost(uri.getHost());
        String domain = uri.getHost();

        List<String> warnings = new ArrayList<>();
        try {
            Document document = fetchDocument(uri.toString());

            String title = firstNonBlank(
                    metaContent(document, "meta[property=og:title]"),
                    metaContent(document, "meta[name=twitter:title]"),
                    document.title()
            );
            String description = firstNonBlank(
                    metaContent(document, "meta[property=og:description]"),
                    metaContent(document, "meta[name=description]"),
                    metaContent(document, "meta[name=twitter:description]")
            );
            String siteName = firstNonBlank(
                    metaContent(document, "meta[property=og:site_name]"),
                    metaContent(document, "meta[name=application-name]"),
                    metaContent(document, "meta[name=publisher]")
            );

            // Fecha de publicación desde metadata (ANTES de quitar los <script> JSON-LD).
            String publishedAtIso = normalizeIsoDate(detectPublishedAt(document));

            // Cuerpo principal LIMPIO (analysisContent: lo que se envía a la IA), NO la vista previa.
            stripBoilerplate(document);
            String fullContent = extractContent(document);
            int originalChars = fullContent.length();
            boolean contentWasTruncated = fullContent.length() > MAX_CONTENT_CHARS;
            String analysisContent = contentWasTruncated
                    ? fullContent.substring(0, MAX_CONTENT_CHARS).trim()
                    : fullContent;

            boolean titleExtracted = !title.isBlank();
            boolean descriptionExtracted = !description.isBlank();
            if (!titleExtracted) {
                warnings.add("No se pudo extraer un título de la página; se usará el título manual o uno por defecto.");
            }
            if (analysisContent.length() < MIN_USABLE_CONTENT_CHARS) {
                warnings.add("Se obtuvo poco texto del artículo; el análisis de IA puede ser limitado.");
            }
            if (contentWasTruncated) {
                warnings.add("El contenido fue truncado por límite técnico para el análisis.");
            }
            if (publishedAtIso == null) {
                warnings.add("No se detectó una fecha de publicación clara en la metadata de la página.");
            }

            log.info("Extracción URL host={} titleOk={} analysisLen={} (orig={}) publishedAtDetected={} warnings={}",
                    domain, titleExtracted, analysisContent.length(), originalChars,
                    publishedAtIso != null, warnings.size());

            return new BestEffortExtraction(
                    true, domain,
                    nullIfBlank(title), nullIfBlank(description), nullIfBlank(siteName),
                    titleExtracted, descriptionExtracted,
                    nullIfBlank(analysisContent), publishedAtIso, contentWasTruncated, analysisContent.length(),
                    List.copyOf(warnings)
            );
        } catch (RuntimeException ex) {
            warnings.add("No se pudo descargar o procesar el contenido del enlace; la noticia se guarda con los datos disponibles.");
            log.info("Extracción URL host={} falló: {}", domain, ex.getMessage());
            return new BestEffortExtraction(
                    false, domain,
                    null, null, null,
                    false, false,
                    null, null, false, 0,
                    List.copyOf(warnings)
            );
        }
    }

    /** Detecta la fecha de publicación desde metadata (JSON-LD, OG, meta tags, time[datetime]). */
    private String detectPublishedAt(Document document) {
        String meta = firstNonBlank(
                metaContent(document, "meta[property=article:published_time]"),
                metaContent(document, "meta[property=og:published_time]"),
                metaContent(document, "meta[name=article:published_time]"),
                metaContent(document, "meta[name=publishdate]"),
                metaContent(document, "meta[name=pubdate]"),
                metaContent(document, "meta[name=date]"),
                metaContent(document, "meta[name=DC.date]"),
                metaContent(document, "meta[name=DC.date.issued]"),
                metaContent(document, "meta[itemprop=datePublished]"),
                attr(document.selectFirst("time[datetime]"), "datetime")
        );
        if (!meta.isBlank()) {
            return meta;
        }
        for (Element script : document.select("script[type=application/ld+json]")) {
            java.util.regex.Matcher m = JSONLD_DATE.matcher(script.html());
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        return "";
    }

    /** Normaliza a un ISO-8601 que Neo4j datetime() pueda parsear, o null si no es válido. */
    private String normalizeIsoDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        try { java.time.OffsetDateTime.parse(v); return v; } catch (Exception ignored) { }
        try { java.time.LocalDateTime.parse(v); return v + "Z"; } catch (Exception ignored) { }
        try { java.time.LocalDate.parse(v); return v + "T00:00:00Z"; } catch (Exception ignored) { }
        return null;
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private URI parseAndValidateUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("Ingresá una URL válida.");
        }

        try {
            URI uri = new URI(rawUrl.trim()).normalize();
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("La URL debe usar http o https.");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("La URL no contiene un dominio válido.");
            }
            return uri;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("La URL ingresada no tiene un formato válido.");
        }
    }

    private void validatePublicHost(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("La URL no contiene un dominio válido.");
        }
        if ("localhost".equalsIgnoreCase(host)) {
            throw new IllegalArgumentException("No se permiten enlaces locales o privados.");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isPrivateAddress(address)) {
                    throw new IllegalArgumentException("No se permiten enlaces locales o privados.");
                }
            }
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("No se pudo resolver el dominio del enlace.");
        }
    }

    private boolean isPrivateAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        if (address instanceof Inet6Address inet6Address) {
            byte[] bytes = inet6Address.getAddress();
            return bytes.length > 0 && (bytes[0] & 0xFE) == 0xFC;
        }

        return false;
    }

    private Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(FETCH_TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_SIZE_BYTES)
                    .followRedirects(true)
                    .get();
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo descargar el contenido del enlace.");
        }
    }

    private void stripBoilerplate(Document document) {
        document.select("script, style, noscript, svg, iframe, form, button, nav, header, footer, aside").remove();
    }

    private SourceMetadata extractSourceMetadata(Document document, URI resolvedUri) {
        String metadataName = firstNonBlank(
                metaContent(document, "meta[property=og:site_name]"),
                metaContent(document, "meta[name=application-name]"),
                metaContent(document, "meta[name=publisher]")
        );

        if (!metadataName.isBlank()) {
            return new SourceMetadata(metadataName, true);
        }

        return new SourceMetadata(resolvedUri.getHost(), false);
    }

    private boolean hasDateMetadata(Document document) {
        return !firstNonBlank(
                metaContent(document, "meta[property=article:published_time]"),
                metaContent(document, "meta[name=pubdate]"),
                metaContent(document, "meta[name=date]"),
                metaContent(document, "meta[property=og:updated_time]"),
                attr(document.selectFirst("time[datetime]"), "datetime")
        ).isBlank();
    }

    private String extractContent(Document document) {
        String articleText = joinParagraphs(document.select("article p"));
        if (articleText.length() >= MIN_USABLE_CONTENT_CHARS) {
            return articleText;
        }

        String mainText = joinParagraphs(document.select("main p, [role=main] p"));
        if (mainText.length() >= MIN_USABLE_CONTENT_CHARS) {
            return mainText;
        }

        String bodyText = joinParagraphs(document.select("body p"));
        if (bodyText.length() >= MIN_USABLE_CONTENT_CHARS) {
            return bodyText;
        }

        Element body = document.body();
        return body == null ? "" : normalizeWhitespace(body.text());
    }

    private String joinParagraphs(Elements paragraphs) {
        Set<String> parts = new LinkedHashSet<>();
        for (Element paragraph : paragraphs) {
            String text = normalizeWhitespace(paragraph.text());
            if (text.length() >= MIN_PARAGRAPH_CHARS) {
                parts.add(text);
            }
        }
        return String.join("\n\n", parts).trim();
    }

    private String metaContent(Document document, String selector) {
        return attr(document.selectFirst(selector), "content");
    }

    private String attr(Element element, String attribute) {
        if (element == null) return "";
        return normalizeWhitespace(element.attr(attribute));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return normalizeWhitespace(value);
            }
        }
        return "";
    }

    private String normalizeWhitespace(String value) {
        if (value == null) return "";
        return value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String abbreviate(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        return value.substring(0, maxChars).trim() + "…";
    }

    public record ExtractedArticle(
            String originalUrl,
            String resolvedUrl,
            String title,
            String content,
            String contentPreview,
            String fetchStatus,
            String sourceName,
            boolean hasSourceMetadata,
            boolean hasDateMetadata,
            List<String> warnings
    ) {}

    private record SourceMetadata(String name, boolean fromMetadata) {}

    /**
     * Metadata best-effort para creación de noticia por URL. Campos null si no se pudieron extraer.
     * content = analysisContent (cuerpo limpio para la IA); description = excerpt para preview.
     */
    public record BestEffortExtraction(
            boolean success,
            String domain,
            String title,
            String description,
            String siteName,
            boolean titleExtracted,
            boolean descriptionExtracted,
            String content,
            String publishedAtIso,
            boolean contentWasTruncated,
            int analysisContentCharCount,
            List<String> warnings
    ) {}
}
