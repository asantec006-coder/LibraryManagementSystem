package com.library.service.online;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BookProvider {
    /**
     * Searches for books matching the given query asynchronously.
     */
    CompletableFuture<List<OnlineBook>> search(String query);
    
    /**
     * Returns the name of the provider (e.g., "Open Library", "Project Gutenberg").
     */
    String getName();
}
