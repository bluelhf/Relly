package io.github.bluelhf.relly.util;

public class Buffers<T> {
    private T previous = null, current;
    public Buffers(T def) {
        this.current = def;
        this.previous = def;
    }

    public void swap() {
        T temp = current;
        current = previous;
        previous = temp;
    }

    public T get() {
        return current;
    }

    public void set(T newCurrent) {
        this.current = newCurrent;
    }

    public boolean same() {
        return current.equals(previous);
    }
}
