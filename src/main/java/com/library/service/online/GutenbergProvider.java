package com.library.service.online;

import com.fasterxml.jackson.databind.JsonNode;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Connects to Project Gutenberg via the Gutendex API.
 * Provides direct download links for EPUBs.
 */
public class GutenbergProvider extends AbstractBookProvider {
    
    private static final String SEARCH_URL = "https://gutendex.com/books/?search=";

    @Override
    protected String buildSearchUrl(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return SEARCH_URL + encodedQuery;
    }

    @Override
    public String getName() {
        return "Project Gutenberg";
    }

    @Override
    protected List<OnlineBook> parseResponse(String json) {
        List<OnlineBook> books = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode results = root.path("results");
            
            for (JsonNode doc : results) {
                OnlineBook book = new OnlineBook();
                book.setSource(getName());
                
                // Parse Title
                JsonNode titleNode = doc.path("title");
                book.setTitle(titleNode.isMissingNode() || titleNode.isNull() ? "Unknown Title" : titleNode.asText());
                
                // Parse Author
                JsonNode authors = doc.path("authors");
                if (authors.isArray() && !authors.isEmpty()) {
                    JsonNode nameNode = authors.get(0).path("name");
                    book.setAuthor(nameNode.isMissingNode() || nameNode.isNull() ? "Unknown Author" : nameNode.asText());
                } else {
                    book.setAuthor("Unknown Author");
                }
                
                // Parse Formats (for downloads and covers)
                JsonNode formats = doc.path("formats");
                if (formats.isObject()) {
                    // Try to find an EPUB download link
                    if (formats.hasNonNull("application/epub+zip")) {
                        book.setDownloadUrl(formats.get("application/epub+zip").asText());
                        book.setDownloadFormat("epub");
                    } else if (formats.hasNonNull("application/pdf")) {
                        book.setDownloadUrl(formats.get("application/pdf").asText());
                        book.setDownloadFormat("pdf");
                    }
                    
                    // Try to find a cover image
                    if (formats.hasNonNull("image/jpeg")) {
                        book.setCoverUrl(formats.get("image/jpeg").asText());
                    }
                    
                    // Try to find a Read Online link
                    if (formats.hasNonNull("text/html")) {
                        book.setReadOnlineUrl(formats.get("text/html").asText());
                    }
                }
                
                books.add(book);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return books;
    }
}
