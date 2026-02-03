package com.moneytransfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        loadDotenvIfPresent();
        SpringApplication.run(Application.class, args);
    }

    // Dotenv loader
    private static void loadDotenvIfPresent() {
        Path[] candidates = new Path[]{Paths.get(".env"), Paths.get("..", ".env")};
        for (Path p : candidates) {
            if (Files.exists(p) && Files.isRegularFile(p)) {
                System.out.println("Loading .env from: " + p.toAbsolutePath());
                try (Stream<String> lines = Files.lines(p)) {
                    lines.map(String::trim)
                         .filter(l -> !l.isEmpty())
                         .filter(l -> !l.startsWith("#"))
                         .forEach(l -> {
                             int idx = l.indexOf('=');
                             if (idx <= 0) return;
                             String key = l.substring(0, idx).trim();
                             String val = l.substring(idx + 1).trim();
                             // remove surrounding quotes if present
                             if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                 val = val.substring(1, val.length() - 1);
                             }
                             // only set if not provided via system properties or env already
                             if (System.getProperty(key) == null && System.getenv(key) == null) {
                                 System.setProperty(key, val);
                             }
                         });
                } catch (IOException e) {
                    System.err.println("Failed to read .env file " + p + ": " + e.getMessage());
                }
                // stop after first found .env
                return;
            }
        }
    }
}
