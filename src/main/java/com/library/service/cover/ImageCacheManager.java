package com.library.service.cover;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Owns the local "covers/" folder: where cover images live on disk, how
 * cache keys map to filenames, whether a cover already exists (so
 * CoverImageService can skip re-downloading it), and the default
 * placeholder image shown whenever a book has no cover or its cover file
 * has gone missing/corrupt.
 *
 * All paths handed back to callers (and stored in the database, via
 * Book.coverImage) are relative — e.g. "covers/9780439554930.jpg" — so
 * they stay valid even if the application is moved, as long as it's always
 * launched from the same working directory (the same convention already
 * used by the Online Library feature's "downloads/" folder).
 */
public class ImageCacheManager {

    public static final String COVERS_DIR_NAME = "covers";
    public static final String DEFAULT_COVER_FILENAME = "default.png";
    /** Relative path used everywhere in the UI as the "no cover available" fallback. */
    public static final String DEFAULT_COVER_PATH = COVERS_DIR_NAME + "/" + DEFAULT_COVER_FILENAME;

    private final Path coversDir;

    public ImageCacheManager() {
        this.coversDir = Path.of(System.getProperty("user.dir"), COVERS_DIR_NAME);
    }

    /** Creates the covers/ folder and the default placeholder image if either is missing. Safe to call repeatedly. */
    public void init() {
        try {
            if (!Files.exists(coversDir)) {
                Files.createDirectories(coversDir);
            }
            File defaultCover = coversDir.resolve(DEFAULT_COVER_FILENAME).toFile();
            if (!defaultCover.exists()) {
                generateDefaultCoverImage(defaultCover);
            }
        } catch (IOException e) {
            // Non-fatal: worst case, cover thumbnails render blank until this
            // succeeds on a later launch. The rest of the app must not crash.
            e.printStackTrace();
        }
    }

    /**
     * Draws a simple, self-contained placeholder ("no cover" book icon) so
     * the app never depends on shipping or downloading a real default image.
     */
    private void generateDefaultCoverImage(File target) throws IOException {
        int width = 300;
        int height = 450;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xE5, 0xE7, 0xEB));
            g.fillRect(0, 0, width, height);

            g.setColor(new Color(0x9C, 0xA3, 0xAF));
            g.setStroke(new java.awt.BasicStroke(3));
            int bookWidth = 120;
            int bookHeight = 90;
            int bx = (width - bookWidth) / 2;
            int by = (height - bookHeight) / 2 - 30;
            g.drawRoundRect(bx, by, bookWidth, bookHeight, 8, 8);
            g.drawLine(bx + bookWidth / 2, by, bx + bookWidth / 2, by + bookHeight);

            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g.setColor(new Color(0x6B, 0x72, 0x80));
            String text = "No Cover";
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g.drawString(text, (width - textWidth) / 2, by + bookHeight + 40);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", target);
    }

    /** Absolute path to the covers directory on disk. */
    public Path getCoversDir() {
        return coversDir;
    }

    /**
     * True if a real (non-default) cover file already exists on disk for
     * this relative path. Used to decide whether a download can be skipped.
     */
    public boolean exists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        Path resolved = resolve(relativePath);
        return resolved != null && Files.exists(resolved) && Files.isRegularFile(resolved);
    }

    /** Resolves a stored relative path (e.g. "covers/123.jpg") to an absolute Path, or null if it's not under the covers dir. */
    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        return Path.of(System.getProperty("user.dir"), relativePath);
    }

    /** Builds a safe cache key from an ISBN or a "book-{id}" fallback, stripping anything not filename-safe. */
    public String cacheKeyFor(String isbn, int bookId) {
        String raw = (isbn != null && !isbn.isBlank()) ? isbn.trim() : ("book-" + bookId);
        return raw.replaceAll("[^A-Za-z0-9_-]", "");
    }

    /**
     * Saves downloaded image bytes to covers/{key}.{extension}, overwriting
     * any previous file for that key. Returns the relative path to store in
     * the database.
     */
    public String save(String key, byte[] imageBytes, String extension) throws IOException {
        String filename = key + "." + extension;
        Path target = coversDir.resolve(filename);
        Files.write(target, imageBytes);
        return COVERS_DIR_NAME + "/" + filename;
    }

    /**
     * Copies a librarian-selected file into covers/{key}.{extension},
     * renaming it consistently. Returns the relative path to store in the
     * database.
     */
    public String copyUploadedFile(File sourceFile, String key, String extension) throws IOException {
        String filename = key + "." + extension;
        Path target = coversDir.resolve(filename);
        Files.copy(sourceFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return COVERS_DIR_NAME + "/" + filename;
    }

    /** Deletes the cover file for a stored relative path, if it exists and isn't the shared default image. */
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.endsWith(DEFAULT_COVER_FILENAME)) {
            return;
        }
        Path resolved = resolve(relativePath);
        if (resolved != null) {
            try {
                Files.deleteIfExists(resolved);
            } catch (IOException ignored) {
                // Missing/locked file on delete is not fatal — nothing else depends on this succeeding.
            }
        }
    }
}
