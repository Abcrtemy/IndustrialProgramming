package lab3;

import lab2.DirectoryIndexer;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: mvn exec:java -pl lab3-database -Dexec.args=\"<directory_path>\"");
            return;
        }

        Path rootDir = Path.of(args[0]);

        System.out.println("Step 1: Indexing " + rootDir.toAbsolutePath());
        DirectoryIndexer indexer = new DirectoryIndexer();
        Map<String, Map<String, Integer>> index = indexer.index(rootDir);
        System.out.printf("         %d file(s) indexed%n%n", index.size());

        System.out.println("Step 2: Connecting to database...");
        try (Connection conn = DbConnection.create()) {
            System.out.println("         Connected.");
            DatabaseLoader loader = new DatabaseLoader(conn);
            loader.initSchema();
            System.out.println("         Schema ready.");
            loader.load(index);
            System.out.println("Done.");
        }
    }
}
