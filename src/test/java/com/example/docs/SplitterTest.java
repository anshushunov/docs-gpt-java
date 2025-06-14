package com.example.docs;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.RecursiveDocumentSplitter;
import dev.langchain4j.model.tokenization.Tokenizer;
import dev.langchain4j.model.openai.tokenization.OpenAiTokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplitterTest {

    @Test
    void splitRespectsSizeAndOverlap() {
        Tokenizer tokenizer = OpenAiTokenizer.gpt4o();
        RecursiveDocumentSplitter splitter = RecursiveDocumentSplitter.builder()
                .chunkSize(10)
                .chunkOverlap(2)
                .tokenizer(tokenizer)
                .build();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("word").append(i).append(' ');
        }
        Document doc = Document.from(sb.toString());
        List<Document> chunks = splitter.split(doc);
        assertTrue(chunks.size() > 1);

        for (Document chunk : chunks) {
            int tokens = tokenizer.estimateTokenCountInText(chunk.text());
            assertTrue(tokens <= 10);
        }

        List<Integer> first = tokenizer.encode(chunks.get(0).text());
        List<Integer> second = tokenizer.encode(chunks.get(1).text());
        int overlap = 2;
        List<Integer> tail = first.subList(first.size() - overlap, first.size());
        List<Integer> head = second.subList(0, overlap);
        assertEquals(tail, head);
    }
}
