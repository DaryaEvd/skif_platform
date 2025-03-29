package ru.nsu.fit.evdokimova.supervisor.utils;

import java.util.concurrent.atomic.AtomicLong;

public class GeneratorId {
    private static final AtomicLong counter = new AtomicLong(0);

    public static Long generateId() {
        return counter.incrementAndGet();
    }
}