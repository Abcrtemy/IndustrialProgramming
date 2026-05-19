package lab2;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: mvn exec:java -pl lab2-indexer -Dexec.args=\"<directory_path>\"");
            return;
        }

        Path rootDir = Path.of(args[0]);
        DirectoryIndexer indexer = new DirectoryIndexer();

        System.out.printf("Indexing: %s%n", rootDir.toAbsolutePath());
        System.out.printf("Threads:  %d%n%n", indexer.getThreadCount());

        long start = System.currentTimeMillis();
        Map<String, Map<String, Integer>> index = indexer.index(rootDir);
        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("Indexed %d file(s) in %d ms%n%n", index.size(), elapsed);

        for (Map.Entry<String, Map<String, Integer>> entry : index.entrySet()) {
            System.out.println("File: " + entry.getKey());
            System.out.println("  Unique words: " + entry.getValue().size());
            entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(e -> System.out.printf("  %-20s %d%n", e.getKey(), e.getValue()));
            System.out.println();
        }

        Map<String, Integer> global = new HashMap<>();
        for (Map<String, Integer> fileWords : index.values()) {
            fileWords.forEach((word, count) -> global.merge(word, count, Integer::sum));
        }

        System.out.println("=== Global Top 10 across all files ===");
        global.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> System.out.printf("%-20s %d%n", e.getKey(), e.getValue()));
    }
}
