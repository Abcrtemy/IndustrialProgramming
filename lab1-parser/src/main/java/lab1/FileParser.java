package lab1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class FileParser {

    private static final String WORD_SPLIT_REGEX = "[^a-zA-Zа-яА-ЯёЁ]+";

    public Map<String, Integer> parse(Path filePath) throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        return Arrays.stream(content.toLowerCase().split(WORD_SPLIT_REGEX))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toMap(
                        word -> word,
                        word -> 1,
                        Integer::sum
                ));
    }
}
