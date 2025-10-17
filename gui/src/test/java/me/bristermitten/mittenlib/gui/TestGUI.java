package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.collections.Maps;
import me.bristermitten.mittenlib.gui.command.Command;
import me.bristermitten.mittenlib.gui.view.TextualView;
import org.jetbrains.annotations.NotNull;

public class TestGUI implements GUIBase<Counter, CounterMessage, TextualView<CounterMessage>, Command<CounterMessage>> {


    @Override
    public @NotNull Counter init() {
        return Counter.create(0);
    }

    @Override
    public @NotNull UpdateResult<Counter, CounterMessage, Command<CounterMessage>> update(Counter counter, CounterMessage message) {
        return message.matchTo(
                increment -> UpdateResult.pure(Counter.create(counter.value() + 1)),
                decrement -> UpdateResult.pure(Counter.create(counter.value() - 1)),
                setValue -> UpdateResult.pure(Counter.create(setValue.value()))
        );
    }


    @Override
    public @NotNull TextualView<CounterMessage> render(Counter counter) {
        return TextualView.of(
                "Counter value: " + counter.value() + "\n" +
                        "Commands:\n" +
                        "1. Increment\n" +
                        "2. Decrement\n" +
                        "3. Set to a specific value (just 42 right now)",
                Maps.of(
                        "1", CounterMessage.Increment(),
                        "2", CounterMessage.Decrement(),
                        "3", CounterMessage.Set(42) // needs better parsing
                )
        );
    }


}
