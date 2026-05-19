package lab3;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class DatabaseLoader {

    private final Connection connection;

    public DatabaseLoader(Connection connection) {
        this.connection = connection;
    }

    public void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS files (
                        id       SERIAL PRIMARY KEY,
                        filename VARCHAR(255) NOT NULL,
                        filepath TEXT         NOT NULL UNIQUE
                    )""");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS words (
                        id   SERIAL PRIMARY KEY,
                        word VARCHAR(255) NOT NULL UNIQUE
                    )""");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS occurrences (
                        file_id INT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
                        word_id INT NOT NULL REFERENCES words(id) ON DELETE CASCADE,
                        count   INT NOT NULL,
                        PRIMARY KEY (file_id, word_id)
                    )""");
        }
    }

    public void load(Map<String, Map<String, Integer>> index) throws SQLException {
        connection.setAutoCommit(false);
        try {
            Map<String, Integer> fileIds = insertFiles(index.keySet());
            Map<String, Integer> wordIds = insertWords(index);
            insertOccurrences(index, fileIds, wordIds);
            connection.commit();
            System.out.printf("Committed: %d files, %d unique words%n", fileIds.size(), wordIds.size());
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private Map<String, Integer> insertFiles(Set<String> filepaths) throws SQLException {
        Map<String, Integer> fileIds = new LinkedHashMap<>();
        String sql = """
                INSERT INTO files (filename, filepath) VALUES (?, ?)
                ON CONFLICT (filepath) DO UPDATE SET filename = EXCLUDED.filename
                RETURNING id""";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String filepath : filepaths) {
                String filename = Path.of(filepath).getFileName().toString();
                ps.setString(1, filename);
                ps.setString(2, filepath);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) fileIds.put(filepath, rs.getInt(1));
                }
            }
        }
        return fileIds;
    }

    private Map<String, Integer> insertWords(Map<String, Map<String, Integer>> index) throws SQLException {
        Set<String> allWords = new HashSet<>();
        index.values().forEach(m -> allWords.addAll(m.keySet()));

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO words (word) VALUES (?) ON CONFLICT (word) DO NOTHING")) {
            for (String word : allWords) {
                ps.setString(1, word);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        Map<String, Integer> wordIds = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, word FROM words");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                wordIds.put(rs.getString("word"), rs.getInt("id"));
            }
        }
        return wordIds;
    }

    private void insertOccurrences(Map<String, Map<String, Integer>> index,
                                   Map<String, Integer> fileIds,
                                   Map<String, Integer> wordIds) throws SQLException {
        String sql = """
                INSERT INTO occurrences (file_id, word_id, count) VALUES (?, ?, ?)
                ON CONFLICT (file_id, word_id) DO UPDATE SET count = EXCLUDED.count""";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Map<String, Integer>> fileEntry : index.entrySet()) {
                int fileId = fileIds.get(fileEntry.getKey());
                for (Map.Entry<String, Integer> wordEntry : fileEntry.getValue().entrySet()) {
                    ps.setInt(1, fileId);
                    ps.setInt(2, wordIds.get(wordEntry.getKey()));
                    ps.setInt(3, wordEntry.getValue());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }
}
