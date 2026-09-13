package dev.doorevisuals.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class FeatureBus {
    private final List<Feature> features = new ArrayList<>();
    private Runnable dirty = () -> {};

    public <T extends Feature> T add(T feature) {
        feature.installToggleBind();
        feature.dirty(() -> this.dirty.run());
        this.features.add(feature);
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

    public <T extends Feature> Optional<T> find(Class<T> type) {
        for (Feature feature : this.features) {
            if (type.isInstance(feature)) {
                return Optional.of(type.cast(feature));
            }
        }

        return Optional.empty();
    }

    public void onDirty(Runnable dirty) {
        this.dirty = dirty == null ? () -> {} : dirty;

        for (Feature feature : this.features) {
            feature.dirty(this.dirty);
        }
    }
}
