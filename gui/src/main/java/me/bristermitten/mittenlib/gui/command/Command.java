package me.bristermitten.mittenlib.gui.command;

import java.util.function.Consumer;

/**
 * A command is an object which performs some side effect and produces a message upon completion.
 *
 * @param <Msg> The type of message produced by this command.
 */
public interface Command<Ctx extends CommandContext, Msg> {
    static <Ctx extends CommandContext, Msg> Command<Ctx, Msg> pure(Msg value) {
        return new PureCommand<>(value);
    }

    @SafeVarargs
    static <Ctx extends CommandContext, Msg> Command<Ctx, Msg> batch(Command<Ctx, Msg>... commands) {
        return (context, dispatch) -> {
            for (Command<Ctx, Msg> cmd : commands) {
                cmd.run(context, dispatch);
            }
        };
    }

    void run(Ctx context, Consumer<Msg> continuation);

    class PureCommand<Ctx extends CommandContext, Msg> implements Command<Ctx, Msg> {
        private final Msg value;

        public PureCommand(Msg value) {
            this.value = value;
        }

        @Override
        public void run(Ctx context, Consumer<Msg> continuation) {
            continuation.accept(value);
        }
    }
}
