package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.codegen.RecordSpec;
import me.bristermitten.mittenlib.codegen.UnionSpec;

@UnionSpec
interface CounterMessageSpec {
    CounterMessageSpec Increment();

    CounterMessageSpec Decrement();

    CounterMessageSpec Set(int value);

    CounterMessageSpec AskForValue();
}

@RecordSpec
interface CounterSpec {
    CounterSpec create(int value);
}