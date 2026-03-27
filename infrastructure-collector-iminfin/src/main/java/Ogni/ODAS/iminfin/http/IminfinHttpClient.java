package Ogni.ODAS.iminfin.http;

import Ogni.ODAS.iminfin.config.IminfinDiscoveryProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class IminfinHttpClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final IminfinDiscoveryProperties properties;

    public IminfinHttpClient(IminfinDiscoveryProperties properties) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        this.properties = properties;
    }

    public String getString(String url) {
        HttpRequest request = baseRequest(url)
                .header("Accept", "text/html,application/json;q=0.9,*/*;q=0.8")
                .GET()
                .build();

        return send(request);
    }

    public JsonNode getJson(String url) {
        HttpRequest request = baseRequest(url)
                .header("Accept", "application/json,text/plain;q=0.9,*/*;q=0.8")
                .GET()
                .build();

        String body = send(request);
        try {
            return objectMapper.readTree(body);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse JSON from URL: " + url, ex);
        }
    }

    private HttpRequest.Builder baseRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(properties.getReadTimeout())
                .header("User-Agent", properties.getUserAgent());
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "iMinfin request failed with status %s for %s".formatted(
                                response.statusCode(), request.uri())
                );
            }
            return response.body();
        } catch (IOException ex) {
            throw new IllegalStateException("iMinfin request failed for URL: " + request.uri(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("iMinfin request interrupted for URL: " + request.uri(), ex);
        }
    }
}
