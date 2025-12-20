package me.bristermitten.mittenlib.demo;

import me.bristermitten.mittenlib.codegen.RecordSpec;
import me.bristermitten.mittenlib.codegen.UnionSpec;

@UnionSpec
interface CounterMessageSpec {
    CounterMessageSpec Increment();

    CounterMessageSpec Decrement();

    CounterMessageSpec Set(int value);

    CounterMessageSpec DisplayCount();
}

@RecordSpec
interface CounterSpec {
    CounterSpec create(int value);
}