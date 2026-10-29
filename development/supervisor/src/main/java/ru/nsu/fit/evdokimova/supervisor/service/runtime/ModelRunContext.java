package ru.nsu.fit.evdokimova.supervisor.service.runtime;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.*;

public class ModelRunContext {
    public final long experimentId;
    public final Path directory;
    public final Map<String, Object> parameters;
    public final BooleanSupplier cancelled;
    public final BiConsumer<String, String> update;
    public final Consumer<String> event;
    public volatile boolean cleanupConfirmed = true;
    public ModelRunContext(long id, Path directory, Map<String, Object> parameters,
                           BooleanSupplier cancelled, BiConsumer<String,String> update, Consumer<String> event) {
        this.experimentId=id; this.directory=directory; this.parameters=parameters;
        this.cancelled=cancelled; this.update=update; this.event=event;
    }
    public void check() { if (cancelled.getAsBoolean()) throw new CancellationException("Experiment cancelled"); }
    public void stage(String value) { check(); update.accept("RUNNING", value); }
}
