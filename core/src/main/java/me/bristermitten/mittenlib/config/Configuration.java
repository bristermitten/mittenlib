package me.bristermitten.mittenlib.config;

public class Configuration<T> {
    private final String fileName;
    private final Class<T> type;

    public Configuration(String fileName, Class<T> type) {
        this.fileName = fileName;
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public Class<T> getType() {
        return type;
    }
}
