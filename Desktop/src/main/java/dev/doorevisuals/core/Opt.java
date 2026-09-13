package dev.doorevisuals.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class Opt<T> {
    private final String id;
    private final String label;
    private T value;
    private final T seed;
    private Runnable dirty = () -> {};
    private BooleanSupplier visible = () -> true;

    protected Opt(String id, String label, T seed) {
        this.id = id;
        this.label = label;
        this.seed = seed;
        this.value = seed;
    }

    public String id() {
        return this.id;
    }

    public String label() {
        return this.label;
    }

    public boolean visible() {
        try {
            return this.visible.getAsBoolean();
        } catch (Throwable throwable) {
            return true;
        }
    }

    public <O extends Opt<T>> O visibleWhen(BooleanSupplier condition) {
        this.visible = condition == null ? () -> true : condition;
        return (O)this;
    }

    public T get() {
        return this.value;
    }

    public void set(T next) {
        T t = this.clean(next);
        if (t == null ? this.value != null : !t.equals(this.value)) {
            this.value = t;
            this.dirty.run();
        }
    }

    public void reset() {
        this.set(this.seed);
    }

    public T seed() {
        return this.seed;
    }

    public boolean isDefault() {
        T t = this.seed;
        T t1 = this.value;
        return t == null ? t1 == null : t.equals(t1);
    }

    public void onDirty(Runnable dirty) {
        this.dirty = dirty == null ? () -> {} : dirty;
    }

    protected T clean(T value) {
        return value;
    }

    public abstract JsonElement json();

    public abstract void read(JsonElement var1);

    public static final class Flag extends Opt<Boolean> {
        public Flag(String id, String label, boolean seed) {
            super(id, label, seed);
        }

        public void flip() {
            this.set(!(Boolean)this.get());
        }

        @Override
        public JsonElement json() {
            return new JsonPrimitive((Boolean)this.get());
        }

        @Override
        public void read(JsonElement element) {
            if (element != null && element.isJsonPrimitive()) {
                this.set(element.getAsBoolean());
            }
        }
    }

    public static final class Key extends Opt<Integer> {
        public Key(String id, String label, int seed) {
            super(id, label, seed);
        }

        @Override
        public JsonElement json() {
            return new JsonPrimitive((Number)this.get());
        }

        @Override
        public void read(JsonElement element) {
            if (element != null && element.isJsonPrimitive()) {
                this.set(element.getAsInt());
            }
        }
    }

    public static final class Num extends Opt<Double> {
        private final double min;
        private final double max;
        private final double step;

        public Num(String id, String label, double seed, double min, double max, double step) {
            super(id, label, seed);
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public double min() {
            return this.min;
        }

        public double max() {
            return this.max;
        }

        public float f() {
            return ((Double)this.get()).floatValue();
        }

        public int i() {
            return ((Double)this.get()).intValue();
        }

        protected Double clean(Double value) {
            double d0 = Math.max(this.min, Math.min(this.max, value));
            return Math.round(d0 / this.step) * this.step;
        }

        @Override
        public JsonElement json() {
            return new JsonPrimitive((Number)this.get());
        }

        @Override
        public void read(JsonElement element) {
            if (element != null && element.isJsonPrimitive()) {
                this.set(element.getAsDouble());
            }
        }
    }

    public static final class Pick extends Opt<String> {
        private final List<String> options;

        public Pick(String id, String label, String seed, String... options) {
            super(id, label, seed);
            this.options = new ArrayList<>(List.of(options));
        }

        public List<String> options() {
            return this.options;
        }

        public void replaceOptions(List<String> next) {
            if (next != null && !next.isEmpty()) {
                this.options.clear();
                this.options.addAll(next);
                if (!this.options.contains(this.get())) {
                    this.set(this.options.getFirst());
                }
            }
        }

        public void next() {
            int i = this.options.indexOf(this.get());
            this.set(this.options.get((i + 1) % this.options.size()));
        }

        protected String clean(String value) {
            return this.options.contains(value) ? value : this.options.getFirst();
        }

        @Override
        public JsonElement json() {
            return new JsonPrimitive((String)this.get());
        }

        @Override
        public void read(JsonElement element) {
            if (element != null && element.isJsonPrimitive()) {
                this.set(element.getAsString());
            }
        }
    }

    public static final class Text extends Opt<String> {
        public Text(String id, String label, String seed) {
            super(id, label, seed == null ? "" : seed);
        }

        protected String clean(String value) {
            return value == null ? "" : value;
        }

        @Override
        public JsonElement json() {
            return new JsonPrimitive((String)this.get());
        }

        @Override
        public void read(JsonElement element) {
            if (element != null && element.isJsonPrimitive()) {
                this.set(element.getAsString());
            }
        }
    }

    public static final class Tint extends Opt<Integer> {
        public Tint(String id, String label, int seed) {
            super(id, label, seed);
        }

        @Override
        public JsonElement json() {
            return new JsonPrimitive((Number)this.get());
        }

        @Override
        public void read(JsonElement element) {
            if (element != null && element.isJsonPrimitive()) {
                this.set(element.getAsInt());
            }
        }
    }
}
