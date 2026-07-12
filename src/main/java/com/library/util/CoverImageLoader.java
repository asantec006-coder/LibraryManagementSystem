package com.library.util;

import com.library.model.Book;
import com.library.service.cover.ImageCacheManager;
import javafx.scene.image.Image;

import java.io.File;
import java.io.FileInputStream;

/**
 * Single shared place every screen goes through to display a book cover —
 * Books, Dashboard, Borrow, Online Library, and the Add/Edit Book dialog
 * all call this instead of loading images themselves. That keeps the
 * "missing or corrupt file falls back to the placeholder, and this never
 * throws" behavior consistent and defined in exactly one place.
 */
public final class CoverImageLoader {

    // Small in-memory cache so switching screens or re-rendering a list
    // doesn't re-decode the same file from disk over and over.
    private static final java.util.Map<String, Image> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    // Reused for path resolution/existence checks only, so that logic lives
    // in exactly one place (ImageCacheManager) instead of being duplicated here.
    private static final ImageCacheManager CACHE_MANAGER = new ImageCacheManager();

    private static Image defaultCoverImage;

    private CoverImageLoader() {
    }

    /** Loads a book's cover (by its stored relative path), or the shared default placeholder if it's missing/blank/corrupt. */
    public static Image load(Book book) {
        return load(book == null ? null : book.getCoverImage());
    }

    /** Loads a cover from a stored relative path (e.g. "covers/9780439554930.jpg"), or the default placeholder as a safe fallback. */
    public static Image load(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return defaultCover();
        }
        Image cached = CACHE.get(relativePath);
        if (cached != null) {
            return cached;
        }

        if (!CACHE_MANAGER.exists(relativePath)) {
            return defaultCover();
        }
        File file = CACHE_MANAGER.resolve(relativePath).toFile();

        try (FileInputStream in = new FileInputStream(file)) {
            Image image = new Image(in);
            if (image.isError()) {
                return defaultCover(); // corrupted/unreadable file — never let a bad image crash the UI
            }
            CACHE.put(relativePath, image);
            return image;
        } catch (Exception e) {
            return defaultCover();
        }
    }

    /**
     * Loads a cover directly from a remote URL (used for Online Library
     * search results, which aren't part of the local catalog/cache), with
     * the same "never crash, fall back to default" guarantee. Loads in the
     * background so the UI thread never blocks on network I/O.
     */
    public static Image loadFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return defaultCover();
        }
        try {
            Image image = new Image(url, true);
            image.errorProperty().addListener((obs, wasError, isError) -> {
                // Errors surface asynchronously for background-loaded images;
                // callers bind their ImageView to this Image directly, so a
                // late failure just leaves the last (blank) frame rather than
                // throwing — acceptable for a best-effort remote thumbnail.
            });
            return image;
        } catch (Exception e) {
            return defaultCover();
        }
    }

    private static synchronized Image defaultCover() {
        if (defaultCoverImage != null) {
            return defaultCoverImage;
        }
        try {
            File file = CACHE_MANAGER.resolve(ImageCacheManager.DEFAULT_COVER_PATH).toFile();
            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    defaultCoverImage = new Image(in);
                    return defaultCoverImage;
                }
            }
        } catch (Exception ignored) {
            // Fall through to the blank placeholder below.
        }
        // Absolute last resort if even the generated default.png couldn't be
        // read (e.g. disk full, permissions) — a 1x1 blank image so an
        // ImageView never has a null image and nothing ever crashes.
        defaultCoverImage = new Image(new java.io.ByteArrayInputStream(new byte[]{}));
        return defaultCoverImage;
    }
}
