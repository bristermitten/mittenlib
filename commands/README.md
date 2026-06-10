# Commands Module

The `commands` module provides a light wrapper around **ACF (Aikar's Command Framework)**, tailored for the MittenLib ecosystem.

## Features

- **Guice Integration**: Simplifies the setup of ACF in Guice-heavy applications.
- **Easy Registration**: Register commands, completions, and contexts by simply binding them with a Multibinder.
- **Abstraction**: Wraps complex ACF setup logic into a more developer-friendly API.

## Documentation

For more information on how to use the Commands module, please refer to the **[Commands Documentation](https://bristermitten.github.io/mittenlib/docs/modules/commands)**.

## Installation

```kotlin
dependencies {
    implementation("me.bristermitten:mittenlib-commands:VERSION")
}
```
