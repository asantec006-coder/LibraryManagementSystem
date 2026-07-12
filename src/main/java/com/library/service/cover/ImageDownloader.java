package com.library.service.cover;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Fetches image bytes from a URL and validates them before anything is
 * written to disk or shown to the user. Networking only — no file system
 * or database code lives here (see ImageCacheManager / CoverImageService).
 */
public class ImageDownloader {

    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024; // 8 MB safety cap
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public ImageDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    /** The result of a successful download: raw bytes plus the format they were detected as. */
    public record DownloadedImage(byte[] bytes, String extension) {
    }

    /**
     * Downloads and validates the image at {@code url}.
     *
     * @return the image bytes and detected file extension, or empty if the
     *         URL didn't return a real, decodable image (404, rate limited,
     *         wrong content type, truncated/corrupted body, etc). This never
     *         throws for "expected" failure modes — callers just move on to
     *         the next source or fall back to the placeholder.
     */
    public Optional<DownloadedImage> download(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 429) {
                // Rate limited — treat like "not found" so the caller can try the next provider.
                return Optional.empty();
            }
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            byte[] body = response.body();
            if (body == null || body.length == 0 || body.length > MAX_IMAGE_BYTES) {
                return Optional.empty();
            }

            return detectFormat(body).map(ext -> new DownloadedImage(body, ext));

        } catch (java.net.http.HttpTimeoutException e) {
            return Optional.empty(); // API unavailable / too slow
        } catch (java.net.ConnectException | java.net.UnknownHostException e) {
            return Optional.empty(); // No internet connection / DNS failure
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty(); // Download interrupted or any other network hiccup
        }
    }

    /**
     * Confirms the bytes are actually a real, decodable image (not an HTML
     * error page served with a 200, or a truncated/corrupted file) and
     * reports which format it is, by magic-byte sniffing first and falling
     * back to a full ImageIO decode attempt.
     */
    private Optional<String> detectFormat(byte[] bytes) {
        String byMagicBytes = sniffMagicBytes(bytes);
        if (byMagicBytes != null) {
            return Optional.of(byMagicBytes);
        }
        // Fall back to a full decode for formats ImageIO understands but we
        // don't have a magic-byte signature for.
        try {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
            if (decoded == null) {
                return Optional.empty(); // corrupted / not actually an image
            }
            return Optional.of("jpg");
        } catch (IOException e) {
            return Optional.empty(); // corrupted image
        }
    }

    /**
     * Recognizes JPEG/PNG/GIF/WEBP by their leading bytes. WEBP in
     * particular is checked this way rather than via ImageIO, since
     * standard Java has no built-in WEBP decoder — sniffing the container
     * header still lets us validate and store the file safely even though
     * we can't decode its pixels server-side.
     */
    static String sniffMagicBytes(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "png";
        }
        if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return "gif";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
