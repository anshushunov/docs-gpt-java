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
