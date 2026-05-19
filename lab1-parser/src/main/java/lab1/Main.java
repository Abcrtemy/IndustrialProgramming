package lab1;

import java.nio.file.Path;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: mvn exec:java -pl lab1-parser -Dexec.args=\"<file_path>\"");
            return;
        }

        Path filePath = Path.of(args[0]);
        FileParser parser = new FileParser();
        Map<String, Integer> wordCount = parser.parse(filePath);

        System.out.printf("File: %s%n", filePath.getFileName());
        System.out.printf("Unique words: %d%n%n", wordCount.size());

        wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .forEach(e -> System.out.printf("%-20s %d%n", e.getKey(), e.getValue()));
    }
}
