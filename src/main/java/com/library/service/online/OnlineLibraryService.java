package com.library.service.online;

import com.library.model.DownloadedBook;
import com.library.repository.DownloadedBookRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;

public class OnlineLibraryService {
    
    private final List<BookProvider> providers;
    private final DownloadedBookRepository repository;
    private final HttpClient httpClient;
    private final Path downloadsDir;

    public OnlineLibraryService(DownloadedBookRepository repository) {
        this.repository = repository;
        this.providers = List.of(new OpenLibraryProvider(), new GutenbergProvider());
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        
        // Ensure downloads directory exists
        this.downloadsDir = Path.of(System.getProperty("user.dir"), "downloads");
        try {
            if (!Files.exists(downloadsDir)) {
                Files.createDirectories(downloadsDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Searches all registered providers concurrently and aggregates results.
     */
    public CompletableFuture<List<OnlineBook>> searchAll(String query) {
        List<CompletableFuture<List<OnlineBook>>> futures = providers.stream()
                .map(provider -> provider.search(query).exceptionally(ex -> {
                    ex.printStackTrace(); // Handle failure gracefully per provider
                    return new ArrayList<>();
                }))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .flatMap(f -> f.join().stream())
                        .collect(Collectors.toList()));
    }

    /**
     * Downloads a book, updating progress via callback, and saves to DB.
     */
    public CompletableFuture<DownloadedBook> downloadBook(OnlineBook book, DoubleConsumer progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Check if already downloaded
                Optional<DownloadedBook> existing = repository.findByDownloadUrl(book.getDownloadUrl());
                if (existing.isPresent()) {
                    progressCallback.accept(1.0);
                    return existing.get();
                }

                // 2. Setup file path
                String safeTitle = book.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
                String extension = book.getDownloadFormat() != null ? "." + book.getDownloadFormat() : ".epub";
                File outputFile = downloadsDir.resolve(safeTitle + extension).toFile();

                // 3. Initiate Download request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(book.getDownloadUrl()))
                        .GET()
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Download failed with HTTP " + response.statusCode());
                }

                long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                
                // 4. Stream and track progress
                try (InputStream is = response.body(); FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalRead = 0;
                    
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                        if (totalBytes > 0) {
                            progressCallback.accept((double) totalRead / totalBytes);
                        } else {
                            progressCallback.accept(-1.0); // Indeterminate
                        }
                    }
                }
                
                progressCallback.accept(1.0); // Complete

                // 5. Save to database
                DownloadedBook dbBook = new DownloadedBook(
                        0, book.getTitle(), book.getAuthor(), book.getSource(),
                        extension.replace(".", ""), outputFile.getAbsolutePath(),
                        book.getDownloadUrl(), LocalDateTime.now()
                );
                
                repository.save(dbBook);
                return dbBook;

            } catch (Exception e) {
                throw new RuntimeException("Failed to download book: " + e.getMessage(), e);
            }
        });
    }

    public List<DownloadedBook> getDownloadedBooks() throws Exception {
        return repository.findAll();
    }
}
