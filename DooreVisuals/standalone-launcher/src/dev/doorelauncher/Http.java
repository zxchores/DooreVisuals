package dev.doorelauncher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

/** Plain HTTP helpers: text, JSON and form posts, and verified file downloads. */
final class Http {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    private Http() {
    }

    record Reply(int status, String body) {
    }

    static Reply get(String url, String... headers) throws IOException {
        return send(builder(url, headers).GET().build());
    }

    static Reply postJson(String url, String json, String... headers) throws IOException {
        return send(builder(url, headers).header("Content-Type", "application/json").header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json)).build());
    }

    static Reply postForm(String url, Map<String, String> form) throws IOException {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
                .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return send(builder(url).header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build());
    }

    static String text(String url) throws IOException {
        Reply r = get(url);
        if (r.status() != 200) {
            throw new IOException("HTTP " + r.status() + " for " + url);
        }
        return r.body();
    }

    private static HttpRequest.Builder builder(String url, String... headers) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60))
            .header("User-Agent", "DooreLauncher/1.0");
        for (int k = 0; k + 1 < headers.length; k += 2) {
            b.header(headers[k], headers[k + 1]);
        }
        return b;
    }

    private static Reply send(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> r = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return new Reply(r.statusCode(), r.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        }
    }

    /** Downloads the url to the file (through a temporary name) and checks the SHA-1 when one is given; three tries. */
    static void download(String url, Path target, String sha1) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            Path part = target.resolveSibling(target.getFileName() + ".part");
            try {
                Files.createDirectories(target.getParent());
                HttpResponse<InputStream> r = CLIENT.send(builder(url).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
                if (r.statusCode() != 200) {
                    r.body().close();
                    throw new IOException("HTTP " + r.statusCode() + " for " + url);
                }
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                try (InputStream in = r.body(); var out = Files.newOutputStream(part)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        digest.update(buf, 0, n);
                    }
                }
                if (sha1 != null && !sha1.isEmpty() && !hex(digest.digest()).equalsIgnoreCase(sha1)) {
                    throw new IOException("Checksum mismatch for " + url);
                }
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException e) {
                last = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IOException(e);
            } finally {
                Files.deleteIfExists(part);
            }
        }
        throw last;
    }

    static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
