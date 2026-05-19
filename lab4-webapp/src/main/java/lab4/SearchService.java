package lab4;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SearchService {

    private final Connection connection;

    public SearchService(Connection connection) {
        this.connection = connection;
    }

    public synchronized List<SearchResult> search(String query) throws SQLException {
        String[] words = query.trim().toLowerCase().split("\\s+");
        List<SearchResult> results = new ArrayList<>();

        String sql = """
                SELECT f.filename, o.count
                FROM occurrences o
                JOIN files f ON f.id = o.file_id
                JOIN words w ON w.id = o.word_id
                WHERE w.word = ?
                ORDER BY o.count DESC
                LIMIT 1
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String word : words) {
                if (word.isBlank()) continue;
                ps.setString(1, word);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        results.add(new SearchResult(word, rs.getString("filename"), rs.getInt("count")));
                    } else {
                        results.add(new SearchResult(word, null, 0));
                    }
                }
            }
        }
        return results;
    }
}
