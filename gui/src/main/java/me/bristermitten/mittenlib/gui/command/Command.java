package me.bristermitten.mittenlib.gui.command;

/**
 * A command is an object which performs some side effect and produces a message upon completion.
 *
 * @param <Msg> The type of message produced by this command.
 */
public interface Command<Msg> {

    static <Msg> Command<Msg> pure(Msg value) {
        return new PureCommand<>(value);
    }

    class PureCommand<Msg> implements Command<Msg> {
        private final Msg value;

        public PureCommand(Msg value) {
            this.value = value;
        }
    }
}
