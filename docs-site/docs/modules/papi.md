# PlaceholderAPI (PAPI) Integration

The `papi` module integrates **PlaceholderAPI** support into MittenLib's string formatting ecosystem.

**Runtime Dependencies:**
* [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (Provided by the server, not shaded)

## Features

With this module installed, any string processed by MittenLib's `MessageFormatter` (or localization utilities) will automatically have PlaceholderAPI placeholders (e.g., `%player_name%`, `%server_online%`) parsed and replaced for the target player.

This allows you to seamlessly use PAPI placeholders in your generated configuration strings without writing boilerplate replacement logic.

## Installation

```kotlin
dependencies {
    implementation("me.bristermitten:mittenlib-papi:VERSION")
}
```
