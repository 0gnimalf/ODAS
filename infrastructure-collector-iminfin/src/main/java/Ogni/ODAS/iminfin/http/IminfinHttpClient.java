package Ogni.ODAS.iminfin.http;

import Ogni.ODAS.iminfin.config.IminfinCollectorProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

@Component
public class IminfinHttpClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration readTimeout;

    public IminfinHttpClient(ObjectMapper objectMapper, IminfinCollectorProperties properties) {
        this.objectMapper = objectMapper;
        this.readTimeout = properties.getReadTimeout();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String getText(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(readTimeout)
                .GET()
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("iMinfin request failed with status " + response.statusCode() + " for " + url);
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("iMinfin request interrupted for " + url + ": " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new IllegalStateException("iMinfin request failed for " + url + ": " + ex.getMessage(), ex);
        }
    }

    public JsonNode getJson(String url) {
        try {
            return objectMapper.readTree(getText(url));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to parse JSON from iMinfin: " + url, ex);
        }
    }

    public String withQuery(String baseUrl, Map<String, ?> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return baseUrl;
        }

        Map<String, Object> sanitized = new LinkedHashMap<>();
        queryParams.forEach((key, value) -> {
            if (value != null) {
                sanitized.put(key, value);
            }
        });

        if (sanitized.isEmpty()) {
            return baseUrl;
        }

        StringJoiner joiner = new StringJoiner("&");
        sanitized.forEach((key, value) -> joiner.add(encode(key) + "=" + encode(String.valueOf(value))));
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + joiner;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
