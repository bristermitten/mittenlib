# annotation-processor

This module defines an Annotation Processor for easy generation of
`Configuration` types.

## Quick Start

Let's say we want to define a Configuration for loading SQL database settings.

We can define a class like this:

```java
import me.bristermitten.mittenlib.config.*;

@Config
public class SQLConfigDTO {
    String host;
    String username;
    String password;
    String database;
    int port = 3306;
    @Nullable
    String tablePrefix;
}
```

Running the annotation processor produces a new type `SQLConfig`, which
contains deserialization logic.

The `SQLConfig` class will also contain getter methods
and copy methods (`withX`) for each field.

The deserialization will load a file with a structure like this:

```yaml
host: localhost
username: root
password: root
database: database
port: 3306
tablePrefix: prefix_
```

If the `host`, `username`, `password`, or `database` fields are not present, an exception will be thrown.
If the `port` field is not present, the default value of `3306` will be used
If the `tablePrefix` field is not present, it will be `null`.

### Naming

Note the similarity between the `SQLConfigDTO` and `SQLConfig`. The API uses the suffix 'DTO' to determine the type of
the generated configuration type.
The suffixes `DAO` and `Template` are also supported.

If you don't want to use the suffix method, you can manually specify the name of the generated class with
`@Config("ClassNameHere")`

Throughout the rest of this guide, and internally, a few names are used though:

- DTO class: The descriptor class for the configuration, annotated with `@Config` that describes the structure of the
  config
- Configuration class: The generated class that contains the deserialization logic.

### Deserialization

You can either deserialize manually using `SQLConfig.deserializeSQLConfig(DeserializationContext)`,
or register the configuration with Guice to have it bound automatically.
Doing this requires another annotation, `@Source`, which defines which file the
config should be read from. For example:

```java
import me.bristermitten.mittenlib.config.*;

@Config
@Source("database.yml")
public class SQLConfigDTO {
    String host;
    String username;
    String password;
    String database;
    int port;
    @Nullable
    String tablePrefix;
}
```

From here we can register it into Guice using the `ConfigModule`:
`new ConfigModule(SQLConfig.CONFIG)`.

You can now inject `SQLConfig` instances anywhere throughout your program.
Recommended usage is to inject a `Provider<SQLConfig>`, as this provides a few advantages:

- The configuration will be lazily loaded, potentially improving performance
- The configuration will be automatically reloaded when the file changes (if using the `FileWatcherModule`)
- The subtype `ConfigProvier` can also be used which gives a little more information, such as the Path to the file that
  was
  loaded.

## Subclasses

DTO Types may extend other DTO types, which is effectively equivalent to
copying all the fields from the super class.
The generated config class will extend from the generated super-config class, if possible.

## Extra Configuration

### Generating `toString` methods

To generate a `toString()` for your Configuration class, add the annotation
`@GenerateToString` to your DTO class. The generated method will use all fields in the config class with a standard
template (`"ClassName{fieldName=fieldValue(,)}"`)

### Different key names

By default, the deserialization methods will use the field name as the key in the file.
However, you may want to write your keys using a different naming convention, eg `lower_snake_case`.
To do this, use the `@NamingPattern` annotation, which can either be applied to a whole class or a specific field.
This takes a `NamingPatterns` enum as a parameter and translates the field name into the specified format.
For example,

```java
@NamingPattern(NamingPatterns.LOWER_KEBAB_CASE)
String someFieldName
```

will use the key `some-field-name`

For further customization, you can manually set the key with `@ConfigName("key-name")`

