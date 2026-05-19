package lab4;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lab1.FileParser;
import lab3.DatabaseLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 10 * 1024 * 1024)
public class UploadServlet extends HttpServlet {

    private final Connection connection;
    private final Path uploadsDir;
    private final FileParser parser = new FileParser();

    public UploadServlet(Connection connection, Path uploadsDir) {
        this.connection = connection;
        this.uploadsDir = uploadsDir;
    }

    @Override
    protected synchronized void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Part part = req.getPart("file");
        String filename = extractFilename(part);

        if (filename == null || filename.isBlank()) {
            resp.sendRedirect("/?msg=" + encode("Не удалось определить имя файла"));
            return;
        }

        Path dest = uploadsDir.resolve(filename);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            Map<String, Integer> wordCount = parser.parse(dest);
            Map<String, Map<String, Integer>> index = Map.of(dest.toAbsolutePath().toString(), wordCount);
            DatabaseLoader loader = new DatabaseLoader(connection);
            loader.load(index);
            resp.sendRedirect("/?msg=" + encode("Файл «" + filename + "» загружен и проиндексирован (" + wordCount.size() + " уникальных слов)"));
        } catch (SQLException e) {
            resp.sendRedirect("/?msg=" + encode("Ошибка БД: " + e.getMessage()));
        }
    }

    private static String extractFilename(Part part) {
        String disposition = part.getHeader("content-disposition");
        if (disposition == null) return null;
        for (String token : disposition.split(";")) {
            token = token.trim();
            if (token.startsWith("filename=")) {
                return token.substring(9).replace("\"", "").trim();
            }
        }
        return null;
    }

    private static String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
