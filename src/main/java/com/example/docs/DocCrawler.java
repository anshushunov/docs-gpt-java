package com.example.docs;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class DocCrawler {
    private static final Logger log = LoggerFactory.getLogger(DocCrawler.class);

    private static String toRawGithubUrl(String url) {
        if (url.startsWith("https://github.com/") && url.contains("/blob/")) {
            return url.replaceFirst("https://github.com/", "https://raw.githubusercontent.com/")
                    .replace("/blob/", "/");
        }
        return url;
    }

    public static void main(String[] args) throws Exception {
        long delayMs = 0;
        if (args.length > 0) {
            try {
                delayMs = Long.parseLong(args[0]);
                if (delayMs < 0) {
                    log.warn("Delay must be non-negative, using 0");
                    delayMs = 0;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid delay value: {}", args[0]);
            }
        }

        Path urlsFile = Paths.get("urls.txt");
        if (!Files.exists(urlsFile)) {
            log.error("urls.txt not found");
            return;
        }
        List<String> urls = Files.readAllLines(urlsFile, StandardCharsets.UTF_8);
        for (String url : urls) {
            url = url.trim();
            if (url.isEmpty()) {
                continue;
            }
            try {
                fetch(url);
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            } catch (Exception e) {
                log.error("Failed to fetch {}", url, e);
            }
        }
    }

    private static void fetch(String url) throws IOException, URISyntaxException {
        log.info("Fetching {}", url);
        String fetchUrl = toRawGithubUrl(url);
        Connection connection = Jsoup.connect(fetchUrl)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0");
        Connection.Response response = connection.execute();

        String contentTypeHeader = Optional.ofNullable(response.contentType()).orElse("").toLowerCase();
        String type;
        String body;
        if (contentTypeHeader.contains("text/html")) {
            Document doc = response.parse();
            doc.select("nav, aside, footer").remove();
            body = FlexmarkHtmlConverter.builder().build().convert(doc.html());
            type = "html";
        } else if (contentTypeHeader.contains("markdown") ||
                contentTypeHeader.contains("text/plain") && fetchUrl.endsWith(".md")) {
            body = response.body();
            type = "markdown";
        } else {
            body = response.body();
            type = "text";
        }

        String fetchedAt = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("source: ").append(url).append('\n');
        sb.append("fetchedAt: ").append(fetchedAt).append('\n');
        sb.append("contentType: ").append(type).append('\n');
        sb.append("---\n\n");
        sb.append(body);

        Path output = Paths.get("corpus/raw").resolve(buildPath(url, contentTypeHeader, type));
        Files.createDirectories(output.getParent());
        Files.writeString(output, sb.toString(), StandardCharsets.UTF_8);
        log.info("Saved {}", output);
    }

    private static Path buildPath(String url, String contentTypeHeader, String type) throws URISyntaxException {
        URI uri = new URI(url);
        String host = Optional.ofNullable(uri.getHost()).orElse("unknown");
        String path = Optional.ofNullable(uri.getPath()).orElse("");
        if (path.isEmpty()) {
            path = "/index";
        }
        if (path.endsWith("/")) {
            path += "index";
        }
        Path p = Paths.get(host, path).normalize();
        String fileName = p.getFileName().toString();
        if (type.equals("html") || type.equals("markdown")) {
            if (!fileName.endsWith(".md")) {
                fileName += ".md";
            }
        } else if (type.equals("text")) {
            if (!fileName.endsWith(".txt")) {
                fileName += ".txt";
            }
        }
        Path parent = p.getParent();
        if (parent == null) {
            return Paths.get(fileName);
        }
        return parent.resolve(fileName);
    }
}
