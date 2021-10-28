package me.bristermitten.mittenlib.collections;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implementations for immutable sets used in {@link Sets}
 */
public class SetImpls {
    private SetImpls() {
    }

    static class Set1<E> extends AbstractSet<E> { //NOSONAR
        private final E e;

        Set1(E e) {
            this.e = e;
        }

        @Override
        public boolean contains(Object o) {
            return e.equals(o);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        @NotNull
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private boolean hasNext = true;

                @Override
                public boolean hasNext() {
                    return hasNext;
                }

                @Override
                public E next() {
                    if (hasNext) {
                        hasNext = false;
                        return e;
                    }
                    throw new NoSuchElementException();
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }
    }
}
