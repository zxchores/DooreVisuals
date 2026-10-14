package dev.doorevisuals.draw;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Пост-обработка кадра: bloom вокруг ярких участков, виньетка и зерно, собранные в цепочку проходов
 * ({@code assets/doorevisuals/post_effect/bloom_*.json}).
 *
 * <p>Движок собирает цепочки при перезагрузке ресурсов, поэтому первые кадры после старта запрос может
 * не находить их — на это рассчитаны повторные попытки. Ошибка самого рендера, наоборот, выключает
 * пост-обработку до перезапуска: лучше кадр без эффекта, чем поломанный рендер.</p>
 */
public final class PostFx {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/PostFx");
    // Внешние цели, которые цепочка читает/пишет: кадр, который рисует движок. Без этого списка
    // загрузчик отвечает "Referenced external targets are not available in this context".
    private static final Set<Identifier> MAIN_TARGET = Set.of(PostEffectProcessor.MAIN);
    private static final Map<String, PostEffectProcessor> CACHE = new HashMap<>();
    private static final long RETRY_MS = 2000L;
    private static final int MAX_ATTEMPTS = 12;

    private static volatile boolean renderFailed = false;
    private static volatile String preset = "Средний";
    private static PostEffectProcessor processor;
    private static String processorId = "";
    private static int attempts = 0;
    private static long nextAttemptAt = 0L;

    private PostFx() {
    }

    public static void setPreset(String value) {
        String next = value == null ? "Средний" : value;
        if (!next.equals(preset)) {
            preset = next;
            processor = null;
            processorId = "";
            attempts = 0;
            nextAttemptAt = 0L;
        }
    }

    public static boolean failed() {
        return renderFailed;
    }

    /** Один проход пост-обработки в самом конце кадра, когда сцена уже нарисована целиком. */
    public static void frame() {
        if (renderFailed) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getFramebuffer() == null) {
            return;
        }

        if (processor == null && !obtain(mc)) {
            return;
        }

        try {
            processor.render(mc.getFramebuffer(), ObjectAllocator.TRIVIAL);
        } catch (Throwable error) {
            renderFailed = true;
            processor = null;
            LOG.error("Пост-обработка выключена после ошибки рендера: {}", error.toString());
        }
    }

    /** Загружает цепочку, повторяя попытки: сразу после старта списки шейдеров ещё не готовы. */
    private static boolean obtain(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        if (attempts >= MAX_ATTEMPTS || now < nextAttemptAt) {
            return false;
        }

        attempts++;
        nextAttemptAt = now + RETRY_MS;

        String id = idFor(preset);
        PostEffectProcessor cached = CACHE.get(id);
        if (cached != null) {
            processor = cached;
            processorId = id;
            return true;
        }

        PostEffectProcessor loaded = load(mc, id);
        if (loaded == null) {
            if (attempts == 1) {
                probe(mc);
            }
            if (attempts >= MAX_ATTEMPTS) {
                LOG.warn("Пост-обработка недоступна: цепочка {} не найдена за {} попыток", id, attempts);
            }
            return false;
        }

        CACHE.put(id, loaded);
        processor = loaded;
        processorId = id;
        LOG.info("Пост-обработка включена: {}", id);
        return true;
    }

    private static String idFor(String preset) {
        return switch (preset) {
            case "Мягкий" -> "bloom_soft";
            case "Сильный" -> "bloom_strong";
            default -> "bloom_medium";
        };
    }

    /**
     * Движок ищет цепочки как {@code <namespace>:<имя>} (файл {@code assets/<namespace>/post_effect/<имя>.json}),
     * но раскладка идентификаторов может отличаться — пробуем несколько форм и пишем, какая сработала.
     */
    private static PostEffectProcessor load(MinecraftClient mc, String id) {
        String[] candidates = {
                "doorevisuals:" + id,
                "doorevisuals:post/" + id,
                "doorevisuals:post_effect/" + id
        };

        for (String candidate : candidates) {
            try {
                PostEffectProcessor loaded = mc.getShaderLoader().loadPostEffect(Identifier.of(candidate), MAIN_TARGET);
                if (loaded != null) {
                    LOG.info("Пост-обработка: цепочка {} загружена", candidate);
                    return loaded;
                }
            } catch (Throwable ignored) {
                // Пробуем следующую форму идентификатора.
            }
        }

        return null;
    }

    /** Показывает в логе, что движок вообще готов отдать: иначе причину не видно без отладчика. */
    private static void probe(MinecraftClient mc) {
        for (String vanilla : new String[] {"minecraft:blur", "minecraft:invert"}) {
            try {
                PostEffectProcessor known = mc.getShaderLoader().loadPostEffect(Identifier.of(vanilla), MAIN_TARGET);
                LOG.info("Пост-обработка: ванильная цепочка {} доступна={}", vanilla, known != null);
            } catch (Throwable error) {
                LOG.info("Пост-обработка: ванильная цепочка {} недоступна ({})", vanilla, error.toString());
            }
        }
    }
}
