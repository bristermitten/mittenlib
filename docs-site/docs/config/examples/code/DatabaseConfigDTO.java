@Config
public class DatabaseConfigDTO {
    String host = "localhost";
    int port = 3306;

    @NotBlank
    String database;
}
