package me.bristermitten.mittenlib.collections;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Implementations for immutable sets used in {@link Sets}
 */
public class SetImpls {
    private SetImpls() {
    }


    abstract static class MLImmutableSet<E> extends AbstractSet<E> {
        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Immutable set");
        }
    }

    static class Set1<E> extends MLImmutableSet<E> { //NOSONAR
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

    static class Set2<E> extends AbstractSet<E> { //NOSONAR
        private final E e1;
        private final E e2;

        Set2(E e1, E e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        @Override
        public boolean contains(Object o) {
            return e1.equals(o) || e2.equals(o);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        @NotNull
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private int count = 0;

                @Override
                public boolean hasNext() {
                    return count < 2;
                }

                @Override
                public E next() {
                    int i = count++;
                    if (i == 0) {
                        return e1;
                    }
                    if (i == 1) {
                        return e2;
                    }
                    throw new NoSuchElementException();
                }
            };
        }

        @Override
        public int size() {
            return 2;
        }
    }

    static class Set3<E> extends AbstractSet<E> { //NOSONAR
        private final E e1;
        private final E e2;
        private final E e3;

        Set3(E e1, E e2, E e3) {
            this.e1 = e1;
            this.e2 = e2;
            this.e3 = e3;
        }

        @Override
        public boolean contains(Object o) {
            return e1.equals(o) || e2.equals(o) || e3.equals(o);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        @NotNull
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private int count = 0;

                @Override
                public boolean hasNext() {
                    return count < 3;
                }

                @Override
                public E next() {
                    int i = count++;
                    if (i == 0) {
                        return e1;
                    }
                    if (i == 1) {
                        return e2;
                    }
                    if (i == 2) {
                        return e3;
                    }
                    throw new NoSuchElementException();
                }
            };
        }

        @Override
        public int size() {
            return 3;
        }
    }

    static class SetN<E> extends MLImmutableSet<E> { //NOSONAR
        private final Set<E> set;

        SetN(Set<E> set) {
            this.set = set;
        }

        @Override
        public boolean contains(Object o) {
            return set.contains(o);
        }

        @Override
        public boolean isEmpty() {
            return set.isEmpty();
        }

        @Override
        @NotNull
        public Iterator<E> iterator() {
            return set.iterator();
        }

        @Override
        public int size() {
            return set.size();
        }
    }

    /**
     * Union of 2 sets
     */
    static class UnionOf<E> extends AbstractSet<E> { //NOSONAR
        private final @Unmodifiable Set<E> first;
        private final @Unmodifiable Set<E> second;
        private final int size;

        UnionOf(Set<E> first, Set<E> second) {
            this.first = first;
            this.second = second;
            this.size = first.size() + second.size();
        }

        @Override
        public boolean contains(Object o) {
            return first.contains(o) || second.contains(o);
        }

        @Override
        public boolean isEmpty() {
            return first.isEmpty() && second.isEmpty();
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        @NotNull
        public Iterator<E> iterator() {
            return Iterators.concat(first.iterator(), second.iterator());
        }
    }
}
