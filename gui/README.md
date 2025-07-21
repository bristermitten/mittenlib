# GUI Module

This module provides a GUI library based on the Elm architecture

Example Usage:

```java

public class CounterGUI extends GUIBase<Counter, CounterCommand, CounterGUI>{
    
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
    
}

```