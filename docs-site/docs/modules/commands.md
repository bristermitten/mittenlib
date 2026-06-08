# Commands

The `commands` module provides a light wrapper around the popular **ACF (Aikar's Command Framework)**.

**Runtime Dependencies:**
* [ACF](https://github.com/aikar/commands/)

## Why use this module?

ACF is a powerful command framework, but integrating it cleanly into a Guice-heavy application can sometimes be messy. This module wraps some of the more complex parts of ACF to make dependency injection and setup smoother within the MittenLib ecosystem.

If you prefer to use a different command framework (like cloud, Brigadier, or Lamp), you do not need this module. However, if you are already using ACF or plan to use it, this module provides some helpful quality-of-life improvements.

## Installation

```kotlin
dependencies {
    implementation("me.bristermitten:mittenlib-commands:VERSION")
}
```
