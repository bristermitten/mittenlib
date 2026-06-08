public class DatabaseConfigSerializer implements SerializationFunction<DatabaseConfig> {
  @Override
  public DataTree apply(DatabaseConfig config, SerializationContext context) {
    Map<String, DataTree> map = new HashMap<>();
    map.put("host", DataTree.string(config.host()));
    map.put("port", DataTree.integer(config.port()));
    map.put("database", DataTree.string(config.database()));
    return DataTree.map(map);
  }
}
