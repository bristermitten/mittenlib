package me.bristermitten.mittenlib.gui.message;

public interface Message<T> {

    static <T> Message<T> pure(T value) {
        return new PureMessage<>(value);
    }

    class PureMessage<T> implements Message<T> {
        private final T value;

        public PureMessage(T value) {
            this.value = value;
        }
    }
}
