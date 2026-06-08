@GeneratedConfig(source = DatabaseConfigDTO.class)
public class DatabaseConfig {
    private final @NonNull String host;
    private final @NonNull int port;
    private final @NonNull String database;

    public DatabaseConfig(final @NonNull String host, final @NonNull int port,
                          final @NonNull String database) {
        this.host = host;
        this.port = port;
        this.database = database;
    }

    public @NonNull String host() {
        return host;
    }

    public @NonNull int port() {
        return port;
    }

    public @NonNull String database() {
        return database;
    }

    public DatabaseConfig withHost(final String host) {
        return new DatabaseConfig(host, this.port, this.database);
    }

    // equals, hashCode, and toString are also generated
}
