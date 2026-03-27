package Ogni.ODAS.iminfin.session.parser;

import Ogni.ODAS.iminfin.profile.IminfinReportProfile;
import Ogni.ODAS.iminfin.session.model.IminfinResolvedReportSession;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IminfinPageSessionParser {

    /**
     * Самый надёжный современный маркер: сообщения фронтенда вида
     * UUID: "ad958966-6bbd-40e0-8b10-a32f5107398f" найден в списке рубрикатора
     */
    private static final Pattern RUBRICATOR_UUID_PATTERN = Pattern.compile(
            "UUID\\s*:\\s*\"([0-9a-fA-F-]{36})\""
    );

    /**
     * Иногда reportId всё ещё лежит прямо в url/iframe/query-параметрах.
     */
    private static final Pattern REPORT_ID_PATTERN = Pattern.compile(
            "(?:^|[?&]|\\b)reportId=([^&\\\"'\\s<>]+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Фоллбек для uuid в url. Менее надёжен, потому что в html встречаются служебные old-uuid customResources.
     * Поэтому используем его только после RUBRICATOR_UUID_PATTERN и reportId.
     */
    private static final Pattern UUID_PARAM_PATTERN = Pattern.compile(
            "(?:^|[?&]|\\b)uuid=([^&\\\"'\\s<>]+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * На новых страницах актуальная версия отчёта видна в сообщениях фронтенда:
     * использую версию отчета из рубрикатора: 015 (27.05.2025 15.32.03.296)
     */
    private static final Pattern RUBRICATOR_VERSION_PATTERN = Pattern.compile(
            "использую\\s+версию\\s+отч[её]та\\s+из\\s+рубрикатора\\s*:\\s*\\d+\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /**
     * Явный dataVersion в query-параметрах.
     */
    private static final Pattern DATA_VERSION_PATTERN = Pattern.compile(
            "(?:^|[?&]|\\b)dataVersion=([^&\\\"'<>\\s]+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Фоллбек для version в query-параметрах.
     */
    private static final Pattern VERSION_PARAM_PATTERN = Pattern.compile(
            "(?:^|[?&]|\\b)version=([^&\\\"'<>\\s]+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Пока reportCode не нужен вызывающему коду, но полезно держать под рукой при отладке.
     */
    private static final Pattern REPORT_CODE_PATTERN = Pattern.compile(
            "код\\s+отч[её]та\\s*:\\s*([A-Za-z0-9_]+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public IminfinResolvedReportSession parse(IminfinReportProfile profile, String html) {
        Document document = Jsoup.parse(html, profile.pageUrl());
        List<String> candidates = collectCandidates(document, html);

        String reportId = extractReportId(candidates)
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to extract iMinfin reportId/uuid from page: " + profile.pageUrl()
                ));

        String uuid = extractUuid(candidates)
                .orElse(reportId);

        String version = extractVersion(candidates)
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to extract iMinfin version from page: " + profile.pageUrl()
                ));

        return new IminfinResolvedReportSession(
                profile.key(),
                profile.pageUrl(),
                profile.redirectBaseUrl(),
                reportId,
                uuid,
                version,
                version,
                OffsetDateTime.now()
        );
    }

    private List<String> collectCandidates(Document document, String html) {
        Set<String> ordered = new LinkedHashSet<>();

        // 1. Сырые html/script-фрагменты — здесь часто лежат самые полезные console.log сообщения.
        ordered.add(html);
        ordered.add(document.outerHtml());

        // 2. Тексты script-элементов.
        for (Element script : document.select("script")) {
            String data = script.data();
            if (!data.isBlank()) {
                ordered.add(data);
            }
            String htmlFragment = script.outerHtml();
            if (!htmlFragment.isBlank()) {
                ordered.add(htmlFragment);
            }
            if (script.hasAttr("src")) {
                ordered.add(script.attr("src"));
                ordered.add(script.absUrl("src"));
            }
        }

        // 3. Iframe обычно содержит embed-адрес или служебные параметры.
        for (Element iframe : document.select("iframe[src]")) {
            ordered.add(iframe.attr("src"));
            ordered.add(iframe.absUrl("src"));
            ordered.add(iframe.outerHtml());
        }

        // 4. Иногда uuid/version утекают через link[href] или другие resource-url.
        for (Element link : document.select("link[href], a[href]")) {
            ordered.add(link.attr("href"));
            ordered.add(link.absUrl("href"));
        }

        return new ArrayList<>(ordered);
    }

    private Optional<String> extractReportId(List<String> candidates) {
        // 1. Явный reportId=...
        Optional<String> fromReportIdParam = candidates.stream()
                .map(text -> extractFirst(text, REPORT_ID_PATTERN))
                .flatMap(Optional::stream)
                .findFirst();
        if (fromReportIdParam.isPresent()) {
            return fromReportIdParam;
        }

        // 2. UUID из логов фронтенда — на текущем iMinfin это основной источник.
        Optional<String> fromRubricatorUuid = candidates.stream()
                .map(text -> extractFirst(text, RUBRICATOR_UUID_PATTERN))
                .flatMap(Optional::stream)
                .findFirst();
        if (fromRubricatorUuid.isPresent()) {
            return fromRubricatorUuid;
        }

        // 3. Самый слабый фоллбек — uuid в url. Используем последним.
        return candidates.stream()
                .map(text -> extractFirst(text, UUID_PARAM_PATTERN))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> extractUuid(List<String> candidates) {
        Optional<String> fromRubricatorUuid = candidates.stream()
                .map(text -> extractFirst(text, RUBRICATOR_UUID_PATTERN))
                .flatMap(Optional::stream)
                .findFirst();
        if (fromRubricatorUuid.isPresent()) {
            return fromRubricatorUuid;
        }

        return candidates.stream()
                .map(text -> extractFirst(text, UUID_PARAM_PATTERN))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> extractVersion(List<String> candidates) {
        // 1. Актуальная версия из рубрикатора.
        Optional<String> fromRubricator = candidates.stream()
                .map(text -> extractFirst(text, RUBRICATOR_VERSION_PATTERN))
                .flatMap(Optional::stream)
                .findFirst();
        if (fromRubricator.isPresent()) {
            return fromRubricator;
        }

        // 2. dataVersion — если вдруг уже есть в query.
        Optional<String> fromDataVersion = candidates.stream()
                .map(text -> extractFirst(text, DATA_VERSION_PATTERN))
                .flatMap(Optional::stream)
                .findFirst();
        if (fromDataVersion.isPresent()) {
            return fromDataVersion;
        }

        // 3. Обычный version=...
        return candidates.stream()
                .map(text -> extractFirst(text, VERSION_PARAM_PATTERN))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @SuppressWarnings("unused")
    private Optional<String> extractReportCode(List<String> candidates) {
        return candidates.stream()
                .map(text -> extractFirst(text, REPORT_CODE_PATTERN))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> extractFirst(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(clean(decode(matcher.group(1))));
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String clean(String value) {
        return value
                .replace("&amp;", "&")
                .replace("&#34;", "\"")
                .trim();
    }
}
