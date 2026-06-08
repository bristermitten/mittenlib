# Configuration System

MittenLib uses an annotation processor to generate high-performance implementation classes from your "Data Transfer Object" (DTO) descriptors.

## Defining a Configuration

A configuration is defined by a class or interface annotated with `@Config`.

```java
@Config
public class DatabaseConfigDTO {
    String host = "localhost";
    int port = 3306;
    
    @NotBlank
    String database;
}
```

## The Generation Process

When you compile your project, MittenLib generates:
1.  **Implementation Class** (`DatabaseConfig`): A final, immutable class with getters and `withX` methods.
2.  **Loader** (`DatabaseConfigLoader`): Logic to transform raw data into your config object.
3.  **Saver** (`DatabaseConfigSaver`): Logic to serialize your config back to a file.
4.  **Validator** (`DatabaseConfigValidator`): Specialized code to check constraints.

## Using the Configuration

Once generated, you can register the config with Guice:

```java
install(new ConfigModule(DatabaseConfig.CONFIG));
```

Then simply inject it:

```java
@Inject
private DatabaseConfig config;
```
