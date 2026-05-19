package lab4;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

public class SearchServlet extends HttpServlet {

    private final SearchService searchService;

    public SearchServlet(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String msg = req.getParameter("msg");
        renderPage(resp, null, msg, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String query = req.getParameter("words");
        if (query == null || query.isBlank()) {
            resp.sendRedirect("/");
            return;
        }
        try {
            List<SearchResult> results = searchService.search(query);
            renderPage(resp, results, null, query);
        } catch (SQLException e) {
            renderPage(resp, null, "Ошибка БД: " + e.getMessage(), query);
        }
    }

    private void renderPage(HttpServletResponse resp, List<SearchResult> results, String msg, String lastQuery)
            throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("""
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                  <meta charset="UTF-8">
                  <title>Поиск слов по файлам</title>
                  <style>
                    body { font-family: Arial, sans-serif; max-width: 860px; margin: 40px auto; padding: 0 20px; color: #222; }
                    h1 { color: #1a1a2e; }
                    h2 { color: #333; margin-top: 30px; }
                    .form-row { display: flex; gap: 8px; align-items: center; margin: 12px 0; }
                    input[type=text] { flex: 1; padding: 9px 12px; font-size: 15px; border: 1px solid #bbb; border-radius: 4px; }
                    input[type=file] { font-size: 14px; }
                    button { padding: 9px 20px; font-size: 15px; background: #0066cc; color: #fff; border: none; border-radius: 4px; cursor: pointer; white-space: nowrap; }
                    button:hover { background: #0052a3; }
                    table { border-collapse: collapse; width: 100%; margin-top: 12px; }
                    th, td { border: 1px solid #ddd; padding: 10px 14px; text-align: left; }
                    th { background: #f5f5f5; font-weight: 600; }
                    tr:hover td { background: #fafafa; }
                    .not-found { color: #999; font-style: italic; }
                    .msg { padding: 10px 14px; border-radius: 4px; margin: 14px 0; }
                    .msg.ok  { background: #e6f4ea; color: #256029; border: 1px solid #a8d5ae; }
                    .msg.err { background: #fdecea; color: #8b1a1a; border: 1px solid #f5c6c6; }
                    hr { border: none; border-top: 1px solid #e0e0e0; margin: 36px 0; }
                  </style>
                </head>
                <body>
                  <h1>Поиск слов по файлам</h1>
                """);

        if (msg != null) {
            boolean isErr = msg.startsWith("Ошибка");
            out.printf("<div class='msg %s'>%s</div>%n", isErr ? "err" : "ok", escape(msg));
        }

        String queryVal = lastQuery != null ? "value=\"" + escape(lastQuery) + "\"" : "";
        out.printf("""
                  <form method="post" action="/search">
                    <div class="form-row">
                      <input type="text" name="words" placeholder="Введите одно или несколько слов через пробел" %s required>
                      <button type="submit">Найти</button>
                    </div>
                  </form>
                """, queryVal);

        if (results != null) {
            out.println("  <h2>Результаты</h2>");
            out.println("  <table><tr><th>Слово</th><th>Файл с наибольшим числом вхождений</th><th>Кол-во</th></tr>");
            for (SearchResult r : results) {
                out.print("  <tr><td>" + escape(r.word()) + "</td>");
                if (r.found()) {
                    out.printf("<td>%s</td><td>%d</td></tr>%n", escape(r.filename()), r.count());
                } else {
                    out.println("<td colspan='2' class='not-found'>не найдено</td></tr>");
                }
            }
            out.println("  </table>");
        }

        out.println("""
                  <hr>
                  <h2>Загрузить файл для индексации</h2>
                  <form method="post" action="/upload" enctype="multipart/form-data">
                    <div class="form-row">
                      <input type="file" name="file" accept=".txt" required>
                      <button type="submit">Загрузить и индексировать</button>
                    </div>
                  </form>
                </body>
                </html>
                """);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;");
    }
}
