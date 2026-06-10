# GUI

The `gui` module provides utilities for _declaratively_ creating and managing interactive inventory menus in Bukkit/Spigot.

:::warning
This module is currently a Work In Progress (WIP). Its API is subject to change.
:::

## Features

The GUI module is based on the **Elm Architecture** (Model-Update-View), providing a predictable and state-driven way to build complex menus.

### Example Usage

Below is a conceptual example of a Counter GUI. Note the use of `@Record` (from the [`record-generator`](./record-generator.md)) to define the State (Model) and Commands (Update).

```java
public class CounterGUI extends GUIBase<Counter, CounterCommand, CounterGUI> {

    @Record
    interface CounterCommandRecord {
        CounterCommandRecord increment();
        CounterCommandRecord decrement();
        CounterCommandRecord set(int value);
    }

    @Record
    interface CounterRecord {
        int value();
    }
    
    // Implementation details for update() and view() would go here
}
```

## Documentation

Full documentation for the GUI module will be expanded as the API stabilizes.

## Installation

```kotlin
dependencies {
    implementation("me.bristermitten:mittenlib-gui:VERSION")
}
```
