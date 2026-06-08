public class DatabaseConfigDeserializer implements DeserializationFunction<DatabaseConfig> {
  @Override
  public Result<DatabaseConfig> apply(DeserializationContext context) {
    DataTree data = context.data();
    return Result.combine(
        data.get("host").map(DataTree::asString).orElse(Result.ok("localhost")),
        data.get("port").map(DataTree::asInt).orElse(Result.ok(3306)),
        data.get("database").map(DataTree::asString).getOrThrow(),
        DatabaseConfig::new
    );
  }
}
