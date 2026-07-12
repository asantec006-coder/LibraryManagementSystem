package com.library.service.cover;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Knows how to ask Open Library and Google Books for a book cover, given an
 * ISBN and/or a title+author. Returns candidate cover URLs only — it does
 * not download or validate image bytes (that's ImageDownloader's job) and
 * it does not touch the file system or the database.
 */
public class BookApiService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public BookApiService() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.mapper = new ObjectMapper();
    }

    /** A single named source to try, in priority order, for a given book. */
    public record CoverCandidate(String url, String source) {
    }

    /**
     * Builds the ordered list of places to look for a cover: Open Library by
     * ISBN, Google Books by ISBN, Open Library by title+author, Google Books
     * by title+author — skipping any step that doesn't apply (e.g. no ISBN).
     * CoverImageService tries these in order and stops at the first one that
     * actually downloads a valid image.
     */
    public List<CoverCandidate> buildCandidates(String isbn, String title, String author) {
        List<CoverCandidate> candidates = new ArrayList<>();
        boolean hasIsbn = isbn != null && !isbn.isBlank();
        boolean hasTitle = title != null && !title.isBlank();

        if (hasIsbn) {
            candidates.add(new CoverCandidate(openLibraryCoverUrlByIsbn(isbn), "Open Library (ISBN)"));
        }
        if (hasIsbn) {
            googleBooksCoverUrl("isbn:" + isbn)
                    .ifPresent(url -> candidates.add(new CoverCandidate(url, "Google Books (ISBN)")));
        }
        if (hasTitle) {
            openLibraryCoverUrlByTitleAuthor(title, author)
                    .ifPresent(url -> candidates.add(new CoverCandidate(url, "Open Library (Title/Author)")));
        }
        if (hasTitle) {
            String query = "intitle:" + title + (author != null && !author.isBlank() ? "+inauthor:" + author : "");
            googleBooksCoverUrl(query)
                    .ifPresent(url -> candidates.add(new CoverCandidate(url, "Google Books (Title/Author)")));
        }
        return candidates;
    }

    /**
     * Open Library serves covers directly by ISBN at a predictable URL — no
     * search call needed. "?default=false" makes it return a real 404
     * instead of a blank placeholder image when there's no cover, which is
     * what lets ImageDownloader tell "found" apart from "not found".
     */
    private String openLibraryCoverUrlByIsbn(String isbn) {
        String encoded = URLEncoder.encode(isbn.trim(), StandardCharsets.UTF_8);
        return "https://covers.openlibrary.org/b/isbn/" + encoded + "-L.jpg?default=false";
    }

    private Optional<String> openLibraryCoverUrlByTitleAuthor(String title, String author) {
        String query = title + (author != null && !author.isBlank() ? " " + author : "");
        String url = "https://openlibrary.org/search.json?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&limit=1";
        return getJson(url).map(root -> {
            JsonNode docs = root.path("docs");
            if (!docs.isArray() || docs.isEmpty()) {
                return null;
            }
            JsonNode first = docs.get(0);
            if (!first.hasNonNull("cover_i")) {
                return null;
            }
            long coverId = first.get("cover_i").asLong();
            return "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg?default=false";
        });
    }

    private Optional<String> googleBooksCoverUrl(String query) {
        String url = "https://www.googleapis.com/books/v1/volumes?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&maxResults=1";
        return getJson(url).map(root -> {
            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                return null;
            }
            JsonNode imageLinks = items.get(0).path("volumeInfo").path("imageLinks");
            JsonNode thumbnail = imageLinks.path("thumbnail");
            if (thumbnail.isMissingNode() || thumbnail.isNull()) {
                return null;
            }
            // Google Books returns http:// links and a low-res "zoom=1" size by default; upgrade both.
            return thumbnail.asText().replace("http://", "https://").replace("zoom=1", "zoom=2");
        });
    }

    private Optional<JsonNode> getJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                return Optional.empty(); // rate limited
            }
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return Optional.of(mapper.readTree(response.body()));
        } catch (Exception e) {
            // No internet, API unavailable, malformed JSON, timeout, etc. — treat as "no candidate", never throw.
            return Optional.empty();
        }
    }
}
