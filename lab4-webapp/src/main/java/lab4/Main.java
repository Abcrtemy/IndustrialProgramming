package lab4;

import jakarta.servlet.MultipartConfigElement;
import lab3.DbConnection;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        Path uploadsDir = Path.of("uploads").toAbsolutePath();
        Files.createDirectories(uploadsDir);

        System.out.println("Connecting to database...");
        Connection searchConn = DbConnection.create();
        Connection uploadConn = DbConnection.create();
        System.out.println("Connected.");

        SearchService searchService = new SearchService(searchConn);
        UploadServlet uploadServlet = new UploadServlet(uploadConn, uploadsDir);
        SearchServlet searchServlet = new SearchServlet(searchService);

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.getConnector();

        Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());

        Wrapper searchWrapper = Tomcat.addServlet(ctx, "search", searchServlet);
        searchWrapper.setLoadOnStartup(1);
        ctx.addServletMappingDecoded("/", "search");
        ctx.addServletMappingDecoded("/search", "search");

        Wrapper uploadWrapper = Tomcat.addServlet(ctx, "upload", uploadServlet);
        uploadWrapper.setLoadOnStartup(1);
        uploadWrapper.setMultipartConfigElement(new MultipartConfigElement(
                System.getProperty("java.io.tmpdir"), 10 * 1024 * 1024, 10 * 1024 * 1024, 0));
        ctx.addServletMappingDecoded("/upload", "upload");

        tomcat.start();
        System.out.printf("Server started: http://localhost:%d%n", PORT);
        tomcat.getServer().await();
    }
}
