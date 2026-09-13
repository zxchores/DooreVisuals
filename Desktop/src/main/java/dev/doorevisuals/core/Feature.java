package dev.doorevisuals.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.lwjgl.glfw.GLFW;

public abstract class Feature {
    private final String id;
    private final String name;
    private final String about;
    private final Category category;
    private final List<Opt<?>> opts = new ArrayList<>();
    private boolean on;
    private boolean listed = true;
    private Runnable dirty = () -> {};
    private Opt.Key toggleBind;
    private boolean bindWasDown;

    protected Feature(String id, String name, String about, Category category, boolean on) {
        this.id = id;
        this.name = name;
        this.about = about;
        this.category = category;
        this.on = on;
    }

    public void installToggleBind() {
        if (this.toggleBind == null) {
            if (this.category != Category.THEME && this.category != Category.CONFIG) {
                if (!"zoom".equals(this.id) && !"free_look".equals(this.id) && !"theme".equals(this.id) && !"config".equals(this.id)) {
                    this.toggleBind = this.opt(new Opt.Key("bind", "\u0411\u0438\u043d\u0434", -1));
                }
            }
        }
    }

    public Opt.Key toggleBind() {
        return this.toggleBind;
    }

    public void pollBind(long window, boolean screenOpen) {
        if (this.toggleBind != null && !screenOpen) {
            int i = (Integer)this.toggleBind.get();
            if (i != -1 && i > 0) {
                boolean flag = GLFW.glfwGetKey(window, i) == 1;
                if (flag && !this.bindWasDown) {
                    this.flip();
                }

                this.bindWasDown = flag;
            } else {
                this.bindWasDown = false;
            }
        } else {
            this.bindWasDown = false;
        }
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String about() {
        return this.about;
    }

    public Category category() {
        return this.category;
    }

    public boolean listed() {
        return this.listed;
    }

    protected void unlist() {
        this.listed = false;
    }

    public boolean on() {
        return this.on;
    }

    public void set(boolean on) {
        if (this.on == on) {
            this.syncRuntime();
        } else {
            this.on = on;
            this.syncRuntime();
            this.dirty.run();
        }
    }

    public void flip() {
        this.set(!this.on);
    }

    public void syncRuntime() {
        try {
            if (this.on) {
                this.enable();
            } else {
                this.disable();
            }
        } catch (Throwable throwable) {
        }
    }

    public List<Opt<?>> opts() {
        return Collections.unmodifiableList(this.opts);
    }

    public List<Opt<?>> visibleOpts() {
        List<Opt<?>> list = new ArrayList<>();

        for (Opt<?> opt : this.opts) {
            if (opt.visible()) {
                list.add(opt);
            }
        }

        return list;
    }

    protected <T extends Opt<?>> T opt(T opt) {
        opt.onDirty(() -> this.dirty.run());
        this.opts.add(opt);
        return opt;
    }

    public void dirty(Runnable dirty) {
        this.dirty = dirty == null ? () -> {} : dirty;
    }

    public void poke() {
        this.dirty.run();
    }

    protected void enable() {
    }

    protected void disable() {
    }
}
