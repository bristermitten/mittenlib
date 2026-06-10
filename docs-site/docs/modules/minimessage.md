# MiniMessage Integration

The `minimessage` module adds automatic support for Adventure's **MiniMessage** format throughout MittenLib's string formatting and localization systems.

**Runtime Dependencies:**
* [MiniMessage](https://docs.adventure.kyori.net/minimessage/)

## Features

When this module is included, any string formatted via MittenLib's `MessageFormatter` will automatically parse MiniMessage tags (e.g., `<red>`, `<bold>`, `<click:run_command:/spawn>`).

This allows you to safely use rich text formatting in your configuration files and localized messages without manually calling `MiniMessage.miniMessage().deserialize()` everywhere in your code.

## Installation

```kotlin
dependencies {
    implementation("me.bristermitten:mittenlib-minimessage:VERSION")
}
```
