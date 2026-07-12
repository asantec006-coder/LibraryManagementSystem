package com.library.service.cover;

import com.library.model.Book;
import com.library.service.BookService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Orchestrates automatic book cover retrieval and manual cover uploads.
 * This is the only class UI code should talk to for cover-related work —
 * it composes BookApiService (where to look), ImageDownloader (how to
 * fetch/validate bytes), ImageCacheManager (where files live on disk and
 * caching), and BookService (persisting the chosen path).
 *
 * Every public method here is safe to call from a background thread; none
 * of them touch JavaFX UI objects.
 */
public class CoverImageService {

    private final BookApiService bookApiService;
    private final ImageDownloader imageDownloader;
    private final ImageCacheManager cacheManager;
    private final BookService bookService;

    public CoverImageService(BookService bookService) {
        this.bookApiService = new BookApiService();
        this.imageDownloader = new ImageDownloader();
        this.cacheManager = new ImageCacheManager();
        this.bookService = bookService;
        this.cacheManager.init();
    }

    public ImageCacheManager getCacheManager() {
        return cacheManager;
    }

    /** Outcome of a cover search, for the UI to react to (status label, preview, enabling Upload). */
    public record CoverSearchResult(boolean found, String coverPath, String source) {
        public static CoverSearchResult notFound() {
            return new CoverSearchResult(false, null, null);
        }
    }

    /**
     * Searches (ISBN first, then title+author, trying Open Library before
     * Google Books at each step) and downloads the first valid cover found,
     * caching it under a key derived from the ISBN (or the book id if there
     * is no ISBN yet). Reuses an existing cached file instead of
     * re-downloading when one is already present for that key.
     *
     * Never throws — network/API/format problems all just result in
     * {@link CoverSearchResult#notFound()} so the caller can fall back to
     * the placeholder and offer manual upload.
     */
    public CoverSearchResult searchAndDownloadCover(String isbn, String title, String author, int bookId) {
        String key = cacheManager.cacheKeyFor(isbn, bookId);

        String cachedPath = findExistingCacheFile(key);
        if (cachedPath != null) {
            return new CoverSearchResult(true, cachedPath, "Cached");
        }

        List<BookApiService.CoverCandidate> candidates = bookApiService.buildCandidates(isbn, title, author);
        for (BookApiService.CoverCandidate candidate : candidates) {
            var downloaded = imageDownloader.download(candidate.url());
            if (downloaded.isPresent()) {
                try {
                    String path = cacheManager.save(key, downloaded.get().bytes(), downloaded.get().extension());
                    return new CoverSearchResult(true, path, candidate.source());
                } catch (IOException e) {
                    // Couldn't write to disk for this candidate — try the next one rather than giving up entirely.
                }
            }
        }
        return CoverSearchResult.notFound();
    }

    /** Checks covers/{key}.{jpg,png,gif,webp} for an already-cached file, without hitting the network. */
    private String findExistingCacheFile(String key) {
        for (String ext : new String[]{"jpg", "png", "gif", "webp"}) {
            String relative = ImageCacheManager.COVERS_DIR_NAME + "/" + key + "." + ext;
            if (cacheManager.exists(relative)) {
                return relative;
            }
        }
        return null;
    }

    /**
     * Validates and copies a librarian-selected file into the covers
     * folder, renamed consistently by ISBN/book id.
     *
     * @throws IllegalArgumentException if the file isn't a real, readable
     *         JPG/PNG/JPEG/WEBP image (extension spoofing, corrupted file, etc).
     */
    public String saveUploadedCover(File sourceFile, String isbn, int bookId) throws IOException {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            throw new IllegalArgumentException("Selected file could not be found.");
        }
        byte[] bytes = Files.readAllBytes(sourceFile.toPath());
        String extension = ImageDownloader.sniffMagicBytes(bytes);
        if (extension == null) {
            throw new IllegalArgumentException(
                    "That file doesn't look like a valid JPG, PNG, JPEG, or WEBP image.");
        }
        String key = cacheManager.cacheKeyFor(isbn, bookId);
        return cacheManager.copyUploadedFile(sourceFile, key, extension);
    }

    /** Detaches and deletes a book's cover, if any (used by the "Remove Cover" button). */
    public void removeCover(String coverPath) {
        cacheManager.delete(coverPath);
    }

    /**
     * Scans every book in the catalog, finds the ones with no cover (or
     * whose cover file has gone missing from disk), and tries to fetch one
     * for each — skipping books that already have a real cover file.
     * Updates the database as it goes. Reports progress via the callback
     * as (booksProcessedSoFar, totalBooksNeedingAttention).
     *
     * Intended to run on a background thread; safe to call from one
     * directly since it does no JavaFX work itself.
     */
    public int fetchMissingCovers(BiConsumer<Integer, Integer> progressCallback) throws SQLException {
        List<Book> allBooks = bookService.getAllBooks();
        List<Book> missing = allBooks.stream()
                .filter(b -> b.getCoverImage() == null || b.getCoverImage().isBlank() || !cacheManager.exists(b.getCoverImage()))
                .toList();

        int updated = 0;
        int total = missing.size();
        int processed = 0;
        for (Book book : missing) {
            CoverSearchResult result = searchAndDownloadCover(book.getIsbn(), book.getTitle(), book.getAuthor(), book.getId());
            if (result.found()) {
                bookService.updateCoverImage(book.getId(), result.coverPath());
                updated++;
            }
            processed++;
            if (progressCallback != null) {
                progressCallback.accept(processed, total);
            }
        }
        return updated;
    }
}
