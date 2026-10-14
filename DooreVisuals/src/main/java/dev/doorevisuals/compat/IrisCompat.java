package dev.doorevisuals.compat;

import java.lang.reflect.Method;
import net.minecraft.client.gui.screen.Screen;

/** Optional Iris bridge. Reflection keeps Iris out of the required mod set. */
public final class IrisCompat {
    private static final String API = "net.irisshaders.iris.api.v0.IrisApi";
    private IrisCompat() {}

    private static Object api() {
        try {
            Class<?> type = Class.forName(API);
            return type.getMethod("getInstance").invoke(null);
        } catch (Throwable ignored) { return null; }
    }

    public static boolean available() { return api() != null; }

    public static boolean shaderPackInUse() {
        Object api = api();
        if (api == null) return false;
        try { return (Boolean)api.getClass().getMethod("isShaderPackInUse").invoke(api); }
        catch (Throwable ignored) { return false; }
    }

    public static Screen openScreen(Object parent) {
        Object api = api();
        if (api == null) return null;
        try {
            Method method = api.getClass().getMethod("openMainIrisScreenObj", Object.class);
            Object screen = method.invoke(api, parent);
            return screen instanceof Screen value ? value : null;
        } catch (Throwable ignored) { return null; }
    }
}
