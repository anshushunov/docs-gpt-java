package com.example.docs;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class PreprocessRunner {

    private static final Logger log = LoggerFactory.getLogger(PreprocessRunner.class);

    private static final Pattern YAML_FRONT_MATTER = Pattern.compile("(?s)^---\\n.*?\\n---\\n");
    private static final Pattern NAV = Pattern.compile("(?is)<nav.*?</nav>");
    private static final Pattern ASIDE = Pattern.compile("(?is)<aside.*?</aside>");
    private static final Pattern FOOTER = Pattern.compile("(?is)<footer.*?</footer>");

    public static void main(String[] args) throws IOException {
        Path rawDir = Paths.get("corpus/raw");
        Path outDir = Paths.get("corpus/chunked");

        OpenAiTokenCountEstimator openAiTokenCountEstimator = new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4_O_2024_11_20);
        DocumentSplitter splitter = DocumentSplitters.recursive(1024, 128, openAiTokenCountEstimator);

        List<Path> files = new ArrayList<>();
        if (Files.exists(rawDir)) {
            try (var stream = Files.walk(rawDir)) {
                stream.filter(p -> p.toString().endsWith(".md"))
                        .forEach(files::add);
            }
        }

        for (Path file : files) {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            text = YAML_FRONT_MATTER.matcher(text).replaceFirst("");
            text = NAV.matcher(text).replaceAll("");
            text = ASIDE.matcher(text).replaceAll("");
            text = FOOTER.matcher(text).replaceAll("");

            Document document = Document.from(text);
            List<TextSegment> chunks = splitter.split(document);
            log.info("splitting {} → {} chunks", file, chunks.size());

            Path relative = rawDir.relativize(file);
            String baseName = relative.toString();
            if (baseName.endsWith(".md")) {
                baseName = baseName.substring(0, baseName.length() - 3);
            }
            for (int i = 0; i < chunks.size(); i++) {
                TextSegment chunk = chunks.get(i);
                int tokens = openAiTokenCountEstimator.estimateTokenCountInText(chunk.text());
                String id = UUID.randomUUID().toString();
                Path chunkPath = outDir.resolve(baseName + "-" + i + ".json");
                Files.createDirectories(chunkPath.getParent());
                String json = "{" +
                        "\"id\":\"" + id + "\"," +
                        "\"text\":\"" + escapeJson(chunk.text()) + "\"," +
                        "\"tokens\":" + tokens + ',' +
                        "\"source\":\"" + relative + "\"," +
                        "\"chunkIndex\":" + i + ',' +
                        "\"totalChunks\":" + chunks.size() +
                        "}";
                Files.writeString(chunkPath, json, StandardCharsets.UTF_8);
            }
        }
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}

