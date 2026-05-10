package io.github.hello09x.fakeplayer.core.manager.naming;

import java.util.Collections;
import java.util.LinkedList;

public class NameSource {

    /**
     */
    private final LinkedList<Integer> names;

    /**
     */
    private int capacity;

    public NameSource(int initializeCapacity) {
        this.capacity = initializeCapacity;
        this.names = new LinkedList<>();
        for (int i = 0; i < initializeCapacity; i++) {
            names.add(i);
        }
    }

    public NameSource() {
        this(0);
    }

    /**
     *
     */
    public int pop() {
        if (names.isEmpty()) {
            var newCapacity = capacity * 2;
            for (int i = capacity; i < newCapacity; i++) {
                names.add(i);
            }
            this.capacity = newCapacity;
        }
        return names.pop();
    }

    /**
     *
     */
    public void push(int i) {
        if (i >= capacity) {
            return;
        }

        if (names.contains(i)) {
            return;
        }

        names.push(i);
        this.sort();
    }

    public void sort() {
        Collections.sort(this.names);
    }


}
