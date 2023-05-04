# MittenLib

![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.bristermitten.me%2Frepository%2Fmaven-public%2Fme%2Fbristermitten%2Fmittenlib-core%2Fmaven-metadata.xml)

A library I use in many of my plugins which provides a wide range of utilities.

[Javadocs](https://knightzmc.github.io/mittenlib/)



## Advantages

MittenLib is different from the common helper frameworks in the Spigot ecosystem in a few ways:

* **Scalability:** MittenLib is strongly based around the [Guice](https://github.com/google/guice/) framework.
  Using Guice makes MittenLib particularly well-suited for larger scale projects, as it
  can make structuring large projects much easier.
* **Extensibility:** MittenLib is highly extensible and modular. By using Guice, different components of the library
  can be individually enabled or removed. Extra functionality can be added in the same way. Each major
  part of MittenLib is split into a separate dependency to keep jar sizes as low as possible.
* **Cross-Version support:** MittenLib supports all Minecraft / Spigot versions including and above 1.8.8
* **Modernity:** Despite its support for old versions, MittenLib is built around the modern practices in Spigot
  development: It uses [Adventure](https://docs.adventure.kyori.net/) throughout its internals, and avoids common
  pitfalls like static abuse as much as possible.
* **Ease of Use:** MittenLib aims to be as simple as possible, whilst keeping its flexible usage

## Current Module List

Modules are generally structured in terms of runtime dependencies
rather than functionality. If a feature requires an extra dependency,
it will be placed in a different module to keep jar sizes as low as possible.

### `core`

#### Runtime Dependencies

* [Guice](https://github.com/google/guice/)
* [Adventure](https://docs.adventure.kyori.net/)

This module is required in every project using MittenLib.
It provides a range of general purpose utilities such as:

* The main entry point into MittenLib, handling Guice bootstrapping
* `Map.of` / `Set.of` methods for pre-Java 9 projects
* File handling methods
* Localization (l18n) utility methods, providing an easy and extensible system for formatting messages and sending them
  to players
* Various useful utility classes: String manipulation, Caching, safe Null and Cast handling à-la Kotlin, and more
* File watching support for automatic reloading of files

### `annotation-processor`

[Full documentation](./annotation-processor/README.md)

#### Runtime Dependencies:

None

This module contains an annotation processor that can automatically
generate classes for deserializing complex data structures.

It is primarily intended for deserializing configuration files,
but can be used for reading any file.

The approach of generating classes has a few powerful advantages over other methods:

* It's fast. Performance will be roughly equivalent to writing the deserialization logic by hand, and much faster than
  when using something like Gson
* It ties in nicely with the functions provided in the `core` module. With just a few lines of code you can make an
  automatically reloading config file with fast parsing and useful error handling
* Generating source code means the behaviour is more transparent than that-of reflection based frameworks like Gson

For example, this code:

```java

@Config
@Source("database.yml")
@NamingPattern(NamingPattern.LOWER_KEBAB_CASE)
public class DatabaseConfigDTO {
    String hostname;
    String database;
    String username;
    String password;
    int port;

    @Nullable
    String tablePrefix;
}
```

will generate a new class `DatabaseConfig` that handles
deserialization, reloading, loading from the file `database.yml`, copying, and
error handling. `DatabaseConfig`'s can be injected anywhere in your application using Guice.

### `commands`

#### Runtime Dependencies

* [ACF](https://github.com/aikar/commands/)

This is a fairly small module, lightly wrapping some of the more messy parts of
ACF. This module will likely not be useful to you if you prefer to use a different command framework,
but I would recommend using it if you do use ACF.

I do not currently plan to recreate a new command framework, as there are already many good ones
out there.

### `minimessage`

#### Runtime Dependencies

- [MiniMessage](https://docs.adventure.kyori.net/minimessage/)

This module adds automatic support for MiniMessage anywhere that Strings are formatted
(i.e. with `MessageFormatter`)

### `papi`

#### Runtime Dependencies

- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (**not shaded**)

This module adds automatic support for placeholder application with PlaceholderAPI anywhere that Strings are formatted

## How to use it

MittenLib is published to my Maven repository making it easy to access.
Take the snippets below, replacing `MODULE` with the module(s) you want to use (you will need 1 separate dependency
per module),
and replace `VERSION` with the latest version.

Note that the `annotation-processor` module will require a slightly different
configuration.

![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.bristermitten.me%2Frepository%2Fmaven-public%2Fme%2Fbristermitten%2Fmittenlib-core%2Fmaven-metadata.xml)

### Maven

```xml

<repositories>
    <repository>
        <id>bristermitten</id>
        <url>https://repo.bristermitten.me/repository/maven-public/</url>
    </repository>
</repositories>
```

```xml

<dependencies>
    <dependency>
        <groupId>me.bristermitten</groupId>
        <artifactId>mittenlib-MODULE</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>
```

### Gradle (Kotlin)

```kotlin
repositories {
    maven("https://repo.bristermitten.me/public/")
}

dependencies {
    implementation("me.bristermitten:mittenlib-MODULE:VERSION")
}
```

### Gradle (Groovy)

```groovy
repositories {
    maven {
        url = 'https://repo.bristermitten.me/repository/maven-public/'
    }
}

dependencies {
    implementation 'me.bristermitten:mittenlib-MODULE:VERSION'
}
```
