package me.bristermitten.mittenlib.demo;

import me.bristermitten.mittenlib.codegen.Record;
import me.bristermitten.mittenlib.codegen.Union;

@Union
interface CounterCommandSpec {
    CounterCommandSpec Increment();

    CounterCommandSpec Decrement();

    CounterCommandSpec Set(int value);
}

@Record
interface CounterSpec {
    CounterSpec create(int value);
}