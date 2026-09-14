package dev.doorevisuals.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class FeatureBus {
    private final List<Feature> features = new ArrayList<>();
    private final Map<Class<?>, Optional<? extends Feature>> lookup = new ConcurrentHashMap<>();
    private Runnable dirty = () -> {};

    public <T extends Feature> T add(T feature) {
        feature.installToggleBind();
        feature.dirty(() -> this.dirty.run());
        this.features.add(feature);
        this.lookup.clear();
        feature.syncRuntime();
        return feature;
    }

    public void syncAllRuntime() {
        for (Feature feature : this.features) {
            feature.syncRuntime();
        }
    }

    public List<Feature> all() {
        return Collections.unmodifiableList(this.features);
    }

    public List<Feature> of(Category category) {
        List<Feature> list = new ArrayList<>();

        for (Feature feature : this.features) {
            if (feature.listed() && feature.category() == category) {
                list.add(feature);
            }
        }

        return List.copyOf(list);
    }

    /**
     * Hot path: called dozens of times per frame from render code, so the linear scan over every
     * registered feature is memoised. The cache is dropped whenever the feature list changes.
     */
    @SuppressWarnings("unchecked")
    public <T extends Feature> Optional<T> find(Class<T> type) {
        Optional<? extends Feature> optional = this.lookup.get(type);
        if (optional == null) {
            optional = Optional.empty();

            for (Feature feature : this.features) {
                if (type.isInstance(feature)) {
                    optional = Optional.of(type.cast(feature));
                    break;
                }
            }

            this.lookup.put(type, optional);
        }

        return (Optional<T>)optional;
    }

    public void onDirty(Runnable dirty) {
        this.dirty = dirty == null ? () -> {} : dirty;

        for (Feature feature : this.features) {
            feature.dirty(this.dirty);
        }
    }
}
