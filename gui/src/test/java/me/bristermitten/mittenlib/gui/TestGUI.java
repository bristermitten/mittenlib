package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.collections.Maps;
import me.bristermitten.mittenlib.gui.view.TextualView;

public class TestGUI implements GUIBase<Counter, CounterCommand, TextualView<CounterCommand>> {


    @Override
    public Counter init() {
        return Counter.create(0);
    }

    @Override
    public Counter update(Counter counter, CounterCommand counterCommand) {
        return counterCommand.matchTo(
                increment -> Counter.create(counter.value() + 1),
                decrement -> Counter.create(counter.value() - 1),
                setValue -> Counter.create(setValue.value())
        );
    }

    @Override
    public TextualView<CounterCommand> render(Counter counter) {
        return TextualView.of(
                "Counter value: " + counter.value() + "\n" +
                "Commands:\n" +
                "1. Increment\n" +
                "2. Decrement\n" +
                "3. Set to a specific value (just 42 right now)",
                Maps.of(
                        "1", CounterCommand.Increment(),
                        "2", CounterCommand.Decrement(),
                        "3", CounterCommand.Set(42) // needs better parsing
                )
        );
    }


}
