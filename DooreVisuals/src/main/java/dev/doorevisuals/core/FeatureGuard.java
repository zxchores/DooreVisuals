package dev.doorevisuals.core;

import dev.doorevisuals.ui.FeatureErrorScreen;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Disables only the feature that failed and reports the first failure once. */
public final class FeatureGuard {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Modules");
    private static final ConcurrentLinkedQueue<Failure> PENDING = new ConcurrentLinkedQueue<>();

    private FeatureGuard() {}

    public static void run(Feature feature, String stage, Runnable action) {
        if (feature == null || action == null) return;
        try {
            action.run();
        } catch (Throwable error) {
            fail(feature, stage, error);
        }
    }

    public static <T extends Feature> void run(T feature, String stage, Consumer<T> action) {
        if (feature == null || !feature.on()) return;
        run(feature, stage, () -> action.accept(feature));
    }

    public static <R> R call(Feature feature, String stage, Supplier<R> action, R fallback) {
        try { return action.get(); }
        catch (Throwable error) { fail(feature, stage, error); return fallback; }
    }

    public static void lifecycle(Feature feature, boolean enabling, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            fail(feature, enabling ? "включение" : "выключение", error);
        }
    }

    private static void fail(Feature feature, String stage, Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) message = error.getClass().getSimpleName();
        message = message.replaceAll("[\\r\\n\\t]", " ").trim();
        if (message.length() > 150) message = message.substring(0, 147) + "...";
        LOG.error("Модуль '{}' ({}) отключён: сбой на этапе {}", feature.name(), feature.id(), stage, error);
        if (feature.on()) {
            try {
                feature.set(false);
            } catch (Throwable disableError) {
                LOG.error("Не удалось корректно выключить модуль {}", feature.id(), disableError);
            }
        }
        PENDING.offer(new Failure(feature.name(), feature.id(), stage, message));
    }

    public static void showPending(MinecraftClient client) {
        if (client == null || client.player == null || client.currentScreen != null) return;
        Failure failure = PENDING.poll();
        if (failure != null) client.setScreen(new FeatureErrorScreen(failure));
    }

    public record Failure(String name, String id, String stage, String message) {}
}
