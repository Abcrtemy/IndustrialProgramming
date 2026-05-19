package lab4;

public record SearchResult(String word, String filename, int count) {
    public boolean found() {
        return filename != null;
    }
}
