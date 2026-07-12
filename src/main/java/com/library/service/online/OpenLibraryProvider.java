package com.library.service.online;

import com.fasterxml.jackson.databind.JsonNode;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Connects to the Open Library Search API.
 * API is called here and JSON is parsed into OnlineBook objects.
 */
public class OpenLibraryProvider extends AbstractBookProvider {
    
    private static final String SEARCH_URL = "https://openlibrary.org/search.json?q=";

    @Override
    protected String buildSearchUrl(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return SEARCH_URL + encodedQuery + "&limit=10";
    }

    @Override
    public String getName() {
        return "Open Library";
    }

    @Override
    protected List<OnlineBook> parseResponse(String json) {
        List<OnlineBook> books = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode docs = root.path("docs");
            
            for (JsonNode doc : docs) {
                OnlineBook book = new OnlineBook();
                book.setSource(getName());
                
                // Parse Title
                JsonNode titleNode = doc.path("title");
                book.setTitle(titleNode.isMissingNode() || titleNode.isNull() ? "Unknown Title" : titleNode.asText());
                
                // Parse Author (array)
                JsonNode authors = doc.path("author_name");
                if (authors.isArray() && !authors.isEmpty()) {
                    book.setAuthor(authors.get(0).asText());
                } else {
                    book.setAuthor("Unknown Author");
                }
                
                // Parse Year
                if (doc.hasNonNull("first_publish_year")) {
                    book.setPublicationYear(doc.path("first_publish_year").asText());
                }
                
                // Parse Cover Image
                if (doc.hasNonNull("cover_i")) {
                    long coverId = doc.get("cover_i").asLong();
                    book.setCoverUrl("https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg");
                }
                
                // Parse Read Online Link
                if (doc.hasNonNull("key")) {
                    book.setReadOnlineUrl("https://openlibrary.org" + doc.get("key").asText());
                }
                
                books.add(book);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return books;
    }
}
