package com.library.service.online;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Base provider handling HTTP client execution and JSON mapper.
 */
public abstract class AbstractBookProvider implements BookProvider {
    
    protected final HttpClient httpClient;
    protected final ObjectMapper mapper;

    public AbstractBookProvider() {
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public CompletableFuture<List<OnlineBook>> search(String query) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildSearchUrl(query)))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseResponse);
    }

    protected abstract String buildSearchUrl(String encodedQuery);

    protected abstract List<OnlineBook> parseResponse(String json);
}
