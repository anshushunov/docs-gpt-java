# docs-gpt-java

Minimal Spring Boot skeleton using JDK 21 and Maven.

## Requirements
- Java 21
- Maven 3

## Running locally
```bash
mvn spring-boot:run
```

The project includes dependencies for LangChain4j, pgvector-jdbc and the OpenAI SDK.

## Crawling documentation

The `DocCrawler` CLI reads `urls.txt`, downloads each URL and stores the result under `corpus/raw/`.

Run it with:

```bash
mvn -q exec:java -Dexec.mainClass=com.example.docs.DocCrawler
```

You can optionally specify a delay between requests (in milliseconds) by
passing a single argument. For example, to pause for one second between
requests:

```bash
mvn -q exec:java -Dexec.mainClass=com.example.docs.DocCrawler -Dexec.args="1000"
```

## Preprocessing Markdown

`PreprocessRunner` splits the downloaded Markdown into token-limited chunks.
It reads every `*.md` under `corpus/raw`, strips YAML front matter and any
`<nav>`, `<aside>` or `<footer>` sections, then uses LangChain4j's recursive
splitter with a chunk size of 1024 tokens, an overlap of 128 tokens and the
GPT-4o tokenizer. Each chunk becomes a JSON file in `corpus/chunked` containing
`id`, `text`, `tokens`, `source`, `chunkIndex` and `totalChunks`. Progress is
logged as `splitting {file} → {n} chunks`.

Run it with:

```bash
mvn -q exec:java -Dexec.mainClass=com.example.docs.PreprocessRunner
```
