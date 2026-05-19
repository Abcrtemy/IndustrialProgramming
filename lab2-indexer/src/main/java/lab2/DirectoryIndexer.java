package lab2;

import lab1.FileParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DirectoryIndexer {

    private final int threadCount;
    private final FileParser parser = new FileParser();

    public DirectoryIndexer() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public DirectoryIndexer(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Recursively indexes all .txt files under rootDir.
     * Returns: absolute file path -> (word -> count)
     */
    public Map<String, Map<String, Integer>> index(Path rootDir) throws IOException, InterruptedException {
        List<Path> files = Files.walk(rootDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".txt"))
                .collect(Collectors.toList());

        Map<String, Map<String, Integer>> result = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (Path file : files) {
            futures.add(executor.submit(() -> {
                try {
                    Map<String, Integer> wordCount = parser.parse(file);
                    result.put(file.toAbsolutePath().toString(), wordCount);
                } catch (IOException e) {
                    System.err.println("Skipping " + file + ": " + e.getMessage());
                }
            }));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        return result;
    }
}
