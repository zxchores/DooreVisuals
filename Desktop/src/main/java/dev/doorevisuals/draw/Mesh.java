package dev.doorevisuals.draw;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Mesh {
    private static final ThreadLocal<Mesh.Frame> FRAME = ThreadLocal.withInitial(Mesh.Frame::new);
    private static final ThreadLocal<Vector3f> CORNER = ThreadLocal.withInitial(Vector3f::new);
    private static final ThreadLocal<float[]> LINK_PTS = ThreadLocal.withInitial(() -> new float[96]);

    private Mesh() {
    }

    public static void cylinder(WorldRenderContext ctx, double x, double y, double z, double r, float h, int segs, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && segs >= 8 && !clear(argb)) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            float f = mesh$frame.y(y);
            float f1 = f + h;

            for (int i = 0; i < segs; i++) {
                double d0 = i * Math.PI * 2.0 / segs;
                double d1 = (i + 1) * Math.PI * 2.0 / segs;
                quad(
                    vertexconsumer,
                    mesh$frame.pose,
                    mesh$frame.x(x + Math.cos(d0) * r),
                    f,
                    mesh$frame.z(z + Math.sin(d0) * r),
                    mesh$frame.x(x + Math.cos(d1) * r),
                    f,
                    mesh$frame.z(z + Math.sin(d1) * r),
                    mesh$frame.x(x + Math.cos(d1) * r),
                    f1,
                    mesh$frame.z(z + Math.sin(d1) * r),
                    mesh$frame.x(x + Math.cos(d0) * r),
                    f1,
                    mesh$frame.z(z + Math.sin(d0) * r),
                    argb
                );
            }
        }
    }

    public static void disc(WorldRenderContext ctx, double x, double y, double z, double inner, double outer, int segs, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && segs >= 6 && outer > inner && !clear(argb)) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            float f = mesh$frame.y(y);

            for (int i = 0; i < segs; i++) {
                double d0 = i * Math.PI * 2.0 / segs;
                double d1 = (i + 1) * Math.PI * 2.0 / segs;
                quad(
                    vertexconsumer,
                    mesh$frame.pose,
                    mesh$frame.x(x + Math.cos(d0) * inner),
                    f,
                    mesh$frame.z(z + Math.sin(d0) * inner),
                    mesh$frame.x(x + Math.cos(d1) * inner),
                    f,
                    mesh$frame.z(z + Math.sin(d1) * inner),
                    mesh$frame.x(x + Math.cos(d1) * outer),
                    f,
                    mesh$frame.z(z + Math.sin(d1) * outer),
                    mesh$frame.x(x + Math.cos(d0) * outer),
                    f,
                    mesh$frame.z(z + Math.sin(d0) * outer),
                    argb
                );
            }
        }
    }

    public static void capsule(WorldRenderContext ctx, double x, double y, double z, double r, float h, int segs, int stacks, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && segs >= 8 && r > 1.0E-4 && h > 1.0E-4F && !clear(argb)) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            double d0 = Math.min(r, h * 0.48);
            float f = (float)(y + d0);
            float f1 = (float)(y + h - d0);
            if (f1 > f + 0.01F) {
                for (int i = 0; i < segs; i++) {
                    double d1 = i * Math.PI * 2.0 / segs;
                    double d2 = (i + 1) * Math.PI * 2.0 / segs;
                    quad(
                        vertexconsumer,
                        mesh$frame.pose,
                        mesh$frame.x(x + Math.cos(d1) * d0),
                        mesh$frame.y(f),
                        mesh$frame.z(z + Math.sin(d1) * d0),
                        mesh$frame.x(x + Math.cos(d2) * d0),
                        mesh$frame.y(f),
                        mesh$frame.z(z + Math.sin(d2) * d0),
                        mesh$frame.x(x + Math.cos(d2) * d0),
                        mesh$frame.y(f1),
                        mesh$frame.z(z + Math.sin(d2) * d0),
                        mesh$frame.x(x + Math.cos(d1) * d0),
                        mesh$frame.y(f1),
                        mesh$frame.z(z + Math.sin(d1) * d0),
                        argb
                    );
                }
            }

            int j = Math.max(4, stacks);
            hemisphere(mesh$frame, vertexconsumer, x, f, z, d0, false, segs, j, argb);
            hemisphere(mesh$frame, vertexconsumer, x, f1, z, d0, true, segs, j, argb);
        }
    }

    public static void torus(WorldRenderContext ctx, double x, double y, double z, double major, double minor, int segs, int tube, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && segs >= 8 && tube >= 6 && major > 0.0 && minor > 0.0 && !clear(argb)) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            float f = mesh$frame.y(y);

            for (int i = 0; i < segs; i++) {
                double d0 = i * Math.PI * 2.0 / segs;
                double d1 = (i + 1) * Math.PI * 2.0 / segs;

                for (int j = 0; j < tube; j++) {
                    double d2 = j * Math.PI * 2.0 / tube;
                    double d3 = (j + 1) * Math.PI * 2.0 / tube;
                    quad(
                        vertexconsumer,
                        mesh$frame.pose,
                        torusX(mesh$frame, x, major, minor, d0, d2),
                        f + (float)(Math.sin(d2) * minor),
                        torusZ(mesh$frame, z, major, minor, d0, d2),
                        torusX(mesh$frame, x, major, minor, d1, d2),
                        f + (float)(Math.sin(d2) * minor),
                        torusZ(mesh$frame, z, major, minor, d1, d2),
                        torusX(mesh$frame, x, major, minor, d1, d3),
                        f + (float)(Math.sin(d3) * minor),
                        torusZ(mesh$frame, z, major, minor, d1, d3),
                        torusX(mesh$frame, x, major, minor, d0, d3),
                        f + (float)(Math.sin(d3) * minor),
                        torusZ(mesh$frame, z, major, minor, d0, d3),
                        argb
                    );
                }
            }
        }
    }

    public static void cone(WorldRenderContext ctx, double x, double y, double z, double rBottom, double rTop, float h, int segs, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && segs >= 6 && h > 0.0F && !clear(argb)) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            float f = mesh$frame.y(y);
            float f1 = f + h;

            for (int i = 0; i < segs; i++) {
                double d0 = i * Math.PI * 2.0 / segs;
                double d1 = (i + 1) * Math.PI * 2.0 / segs;
                quad(
                    vertexconsumer,
                    mesh$frame.pose,
                    mesh$frame.x(x + Math.cos(d0) * rBottom),
                    f,
                    mesh$frame.z(z + Math.sin(d0) * rBottom),
                    mesh$frame.x(x + Math.cos(d1) * rBottom),
                    f,
                    mesh$frame.z(z + Math.sin(d1) * rBottom),
                    mesh$frame.x(x + Math.cos(d1) * rTop),
                    f1,
                    mesh$frame.z(z + Math.sin(d1) * rTop),
                    mesh$frame.x(x + Math.cos(d0) * rTop),
                    f1,
                    mesh$frame.z(z + Math.sin(d0) * rTop),
                    argb
                );
            }
        }
    }

    public static void chainLink(
        WorldRenderContext ctx,
        double x,
        double y,
        double z,
        double tx,
        double ty,
        double tz,
        double nx,
        double ny,
        double nz,
        float half,
        float loop,
        float tube,
        int argb
    ) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && half > 1.0E-4F && loop > 1.0E-4F && tube > 1.0E-4F && !clear(argb)) {
            double d0 = Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (!(d0 < 1.0E-5)) {
                tx /= d0;
                ty /= d0;
                tz /= d0;
                double d1 = nx * tx + ny * ty + nz * tz;
                nx -= d1 * tx;
                ny -= d1 * ty;
                nz -= d1 * tz;
                double d2 = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (d2 < 1.0E-5) {
                    d1 = ty;
                    nx = -d1 * tx;
                    ny = 1.0 - d1 * ty;
                    nz = -d1 * tz;
                    d2 = Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (d2 < 1.0E-5) {
                        return;
                    }
                }

                nx /= d2;
                ny /= d2;
                nz /= d2;
                double d3 = ty * nz - tz * ny;
                double d4 = tz * nx - tx * nz;
                double d5 = tx * ny - ty * nx;
                int i = 6;
                int j = 4;
                int k = (j + 1) * 2 + i * 2;
                float[] afloat = LINK_PTS.get();
                if (afloat.length < k * 3) {
                    afloat = new float[k * 3 + 16];
                    LINK_PTS.set(afloat);
                }

                int l = 0;
                l = linkPoint(afloat, l, x, y, z, tx, ty, tz, d3, d4, d5, -half, loop);

                for (int i1 = 1; i1 <= j; i1++) {
                    float f = (float)i1 / j;
                    l = linkPoint(afloat, l, x, y, z, tx, ty, tz, d3, d4, d5, -half + 2.0F * half * f, loop);
                }

                for (int j2 = 1; j2 <= i; j2++) {
                    double d32 = (Math.PI / 2) - j2 * Math.PI / i;
                    l = linkPoint(afloat, l, x, y, z, tx, ty, tz, d3, d4, d5, half + loop * Math.cos(d32), loop * Math.sin(d32));
                }

                for (int k2 = 1; k2 <= j; k2++) {
                    float f1 = (float)k2 / j;
                    l = linkPoint(afloat, l, x, y, z, tx, ty, tz, d3, d4, d5, half - 2.0F * half * f1, -loop);
                }

                for (int l2 = 1; l2 <= i; l2++) {
                    double d33 = (-Math.PI / 2) - l2 * Math.PI / i;
                    l = linkPoint(afloat, l, x, y, z, tx, ty, tz, d3, d4, d5, -half + loop * Math.cos(d33), loop * Math.sin(d33));
                }

                int i3 = l / 3;
                VertexConsumer vertexconsumer = mesh$frame.buf(Types.glow());
                int j1 = 6;

                for (int k1 = 0; k1 < i3; k1++) {
                    int l1 = (k1 + 1) % i3;
                    double d6 = afloat[k1 * 3];
                    double d7 = afloat[k1 * 3 + 1];
                    double d8 = afloat[k1 * 3 + 2];
                    double d9 = afloat[l1 * 3];
                    double d10 = afloat[l1 * 3 + 1];
                    double d11 = afloat[l1 * 3 + 2];
                    double d12 = d9 - d6;
                    double d13 = d10 - d7;
                    double d14 = d11 - d8;
                    double d15 = Math.sqrt(d12 * d12 + d13 * d13 + d14 * d14);
                    if (!(d15 < 1.0E-5)) {
                        d12 /= d15;
                        d13 /= d15;
                        d14 /= d15;
                        double d16 = d13 * nz - d14 * ny;
                        double d17 = d14 * nx - d12 * nz;
                        double d18 = d12 * ny - d13 * nx;
                        double d19 = Math.sqrt(d16 * d16 + d17 * d17 + d18 * d18);
                        if (d19 < 1.0E-5) {
                            d16 = d13 * d5 - d14 * d4;
                            d17 = d14 * d3 - d12 * d5;
                            d18 = d12 * d4 - d13 * d3;
                            d19 = Math.sqrt(d16 * d16 + d17 * d17 + d18 * d18);
                            if (d19 < 1.0E-5) {
                                continue;
                            }
                        }

                        d16 /= d19;
                        d17 /= d19;
                        d18 /= d19;
                        double d20 = d13 * d18 - d14 * d17;
                        double d21 = d14 * d16 - d12 * d18;
                        double d22 = d12 * d17 - d13 * d16;

                        for (int i2 = 0; i2 < j1; i2++) {
                            double d23 = i2 * Math.PI * 2.0 / j1;
                            double d24 = (i2 + 1) * Math.PI * 2.0 / j1;
                            double d25 = Math.cos(d23);
                            double d26 = Math.sin(d23);
                            double d27 = Math.cos(d24);
                            double d28 = Math.sin(d24);
                            quad(
                                vertexconsumer,
                                mesh$frame.pose,
                                mesh$frame.x(d6 + (d16 * d25 + d20 * d26) * tube),
                                mesh$frame.y(d7 + (d17 * d25 + d21 * d26) * tube),
                                mesh$frame.z(d8 + (d18 * d25 + d22 * d26) * tube),
                                mesh$frame.x(d9 + (d16 * d25 + d20 * d26) * tube),
                                mesh$frame.y(d10 + (d17 * d25 + d21 * d26) * tube),
                                mesh$frame.z(d11 + (d18 * d25 + d22 * d26) * tube),
                                mesh$frame.x(d9 + (d16 * d27 + d20 * d28) * tube),
                                mesh$frame.y(d10 + (d17 * d27 + d21 * d28) * tube),
                                mesh$frame.z(d11 + (d18 * d27 + d22 * d28) * tube),
                                mesh$frame.x(d6 + (d16 * d27 + d20 * d28) * tube),
                                mesh$frame.y(d7 + (d17 * d27 + d21 * d28) * tube),
                                mesh$frame.z(d8 + (d18 * d27 + d22 * d28) * tube),
                                argb
                            );
                        }
                    }
                }
            }
        }
    }

    private static int linkPoint(
        float[] pts, int k, double x, double y, double z, double tx, double ty, double tz, double bx, double by, double bz, double t, double b
    ) {
        pts[k++] = (float)(x + tx * t + bx * b);
        pts[k++] = (float)(y + ty * t + by * b);
        pts[k++] = (float)(z + tz * t + bz * b);
        return k;
    }

    private static float torusX(Mesh.Frame f, double x, double major, double minor, double a, double b) {
        return f.x(x + (major + Math.cos(b) * minor) * Math.cos(a));
    }

    private static float torusZ(Mesh.Frame f, double z, double major, double minor, double a, double b) {
        return f.z(z + (major + Math.cos(b) * minor) * Math.sin(a));
    }

    private static void hemisphere(Mesh.Frame f, VertexConsumer buf, double cx, double cy, double cz, double r, boolean top, int segs, int stacks, int argb) {
        double d0 = top ? 1.0 : -1.0;

        for (int i = 0; i < stacks; i++) {
            double d1 = (Math.PI / 2) * i / stacks;
            double d2 = (Math.PI / 2) * (i + 1) / stacks;

            for (int j = 0; j < segs; j++) {
                double d3 = j * Math.PI * 2.0 / segs;
                double d4 = (j + 1) * Math.PI * 2.0 / segs;
                double d5 = cy + d0 * Math.cos(d1) * r;
                double d6 = cy + d0 * Math.cos(d2) * r;
                double d7 = Math.sin(d1) * r;
                double d8 = Math.sin(d2) * r;
                quad(
                    buf,
                    f.pose,
                    f.x(cx + Math.cos(d3) * d7),
                    f.y(d5),
                    f.z(cz + Math.sin(d3) * d7),
                    f.x(cx + Math.cos(d4) * d7),
                    f.y(d5),
                    f.z(cz + Math.sin(d4) * d7),
                    f.x(cx + Math.cos(d4) * d8),
                    f.y(d6),
                    f.z(cz + Math.sin(d4) * d8),
                    f.x(cx + Math.cos(d3) * d8),
                    f.y(d6),
                    f.z(cz + Math.sin(d3) * d8),
                    argb
                );
            }
        }
    }

    public static void ring(WorldRenderContext ctx, double x, double y, double z, double r, int segs, int argb) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && segs >= 3 && !clear(argb)) {
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.lines());
            float f = mesh$frame.y(y);

            for (int i = 0; i < segs; i++) {
                double d0 = i * Math.PI * 2.0 / segs;
                double d1 = (i + 1) * Math.PI * 2.0 / segs;
                line(
                    vertexconsumer,
                    mesh$frame.pose,
                    mesh$frame.x(x + Math.cos(d0) * r),
                    f,
                    mesh$frame.z(z + Math.sin(d0) * r),
                    mesh$frame.x(x + Math.cos(d1) * r),
                    f,
                    mesh$frame.z(z + Math.sin(d1) * r),
                    argb,
                    2.4F
                );
            }
        }
    }

    public static void box(WorldRenderContext ctx, Box box, int fill, int edge) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null) {
            faces(mesh$frame, box, fill, Types.fill());
            wire(mesh$frame, box, edge, 2.0F, Types.lines());
        }
    }

    public static void cage(WorldRenderContext ctx, Box box, int fill, int edge, float bloom) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null) {
            float f = Math.max(0.25F, bloom);
            Box boxx = box.expand(0.045 * f);
            faces(mesh$frame, boxx, alpha(fill, 0.16F * f), Types.glow());
            faces(mesh$frame, box, fill, Types.holo());
            wire(mesh$frame, box, alpha(edge, 0.32F * f), 5.4F + 1.6F * f, Types.linesTx());
            wire(mesh$frame, box, edge, 1.85F, Types.linesTx());
        }
    }

    private static void faces(Mesh.Frame f, Box box, int argb, RenderLayer type) {
        if (!clear(argb)) {
            float fx = f.x(box.minX);
            float f1 = f.y(box.minY);
            float f2 = f.z(box.minZ);
            float f3 = f.x(box.maxX);
            float f4 = f.y(box.maxY);
            float f5 = f.z(box.maxZ);
            VertexConsumer vertexconsumer = f.buf(type);
            quad(vertexconsumer, f.pose, fx, f1, f2, f3, f1, f2, f3, f1, f5, fx, f1, f5, argb);
            quad(vertexconsumer, f.pose, fx, f4, f2, fx, f4, f5, f3, f4, f5, f3, f4, f2, argb);
            quad(vertexconsumer, f.pose, fx, f1, f2, fx, f4, f2, f3, f4, f2, f3, f1, f2, argb);
            quad(vertexconsumer, f.pose, fx, f1, f5, f3, f1, f5, f3, f4, f5, fx, f4, f5, argb);
            quad(vertexconsumer, f.pose, fx, f1, f2, fx, f1, f5, fx, f4, f5, fx, f4, f2, argb);
            quad(vertexconsumer, f.pose, f3, f1, f2, f3, f4, f2, f3, f4, f5, f3, f1, f5, argb);
        }
    }

    private static void wire(Mesh.Frame f, Box box, int argb, float width, RenderLayer type) {
        if (!clear(argb)) {
            float fx = f.x(box.minX);
            float f1 = f.y(box.minY);
            float f2 = f.z(box.minZ);
            float f3 = f.x(box.maxX);
            float f4 = f.y(box.maxY);
            float f5 = f.z(box.maxZ);
            VertexConsumer vertexconsumer = f.buf(type);
            line(vertexconsumer, f.pose, fx, f1, f2, f3, f1, f2, argb, width);
            line(vertexconsumer, f.pose, fx, f1, f5, f3, f1, f5, argb, width);
            line(vertexconsumer, f.pose, fx, f4, f2, f3, f4, f2, argb, width);
            line(vertexconsumer, f.pose, fx, f4, f5, f3, f4, f5, argb, width);
            line(vertexconsumer, f.pose, fx, f1, f2, fx, f1, f5, argb, width);
            line(vertexconsumer, f.pose, f3, f1, f2, f3, f1, f5, argb, width);
            line(vertexconsumer, f.pose, fx, f4, f2, fx, f4, f5, argb, width);
            line(vertexconsumer, f.pose, f3, f4, f2, f3, f4, f5, argb, width);
            line(vertexconsumer, f.pose, fx, f1, f2, fx, f4, f2, argb, width);
            line(vertexconsumer, f.pose, f3, f1, f2, f3, f4, f2, argb, width);
            line(vertexconsumer, f.pose, fx, f1, f5, fx, f4, f5, argb, width);
            line(vertexconsumer, f.pose, f3, f1, f5, f3, f4, f5, argb, width);
        }
    }

    public static void orb(WorldRenderContext ctx, double x, double y, double z, float radius, int argb) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && radius > 1.0E-4F && !clear(argb)) {
            Quaternionf quaternionf = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
            float f = mesh$frame.x(x);
            float f1 = mesh$frame.y(y);
            float f2 = mesh$frame.z(z);
            Vector3f vector3f = CORNER.get();
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.orb());
            orbCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, -radius, -radius, 0.0F, 1.0F, argb);
            orbCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, radius, -radius, 1.0F, 1.0F, argb);
            orbCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, radius, radius, 1.0F, 0.0F, argb);
            orbCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, -radius, radius, 0.0F, 0.0F, argb);
        }
    }

    private static void orbCorner(
        VertexConsumer buf, Entry pose, Vector3f p, Quaternionf rot, float cx, float cy, float cz, float ox, float oy, float u, float v, int argb
    ) {
        p.set(ox, oy, 0.0F).rotate(rot);
        buf.vertex(pose, cx + p.x, cy + p.y, cz + p.z).texture(u, v).color(argb);
    }

    public static void dome(WorldRenderContext ctx, double x, double y, double z, double r, int segs, int stacks, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && r > 1.0E-4 && segs >= 8 && !clear(argb)) {
            hemisphere(mesh$frame, mesh$frame.buf(type), x, y, z, r, true, segs, Math.max(4, stacks), argb);
        }
    }

    public static void skyDome(
        WorldRenderContext ctx, double x, double y, double z, double r, int segs, int stacks, int zenith, int horizon, int nadir, RenderLayer type
    ) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && r > 1.0E-4 && segs >= 8) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            int i = Math.max(10, stacks * 2);

            for (int j = 0; j < i; j++) {
                double d0 = Math.PI * j / i;
                double d1 = Math.PI * (j + 1) / i;
                float f = (float)j / i;
                float f1 = (float)(j + 1) / i;
                int k = bandTint(zenith, horizon, nadir, f);
                int l = bandTint(zenith, horizon, nadir, f1);
                double d2 = y + Math.cos(d0) * r;
                double d3 = y + Math.cos(d1) * r;
                double d4 = Math.sin(d0) * r;
                double d5 = Math.sin(d1) * r;

                for (int i1 = 0; i1 < segs; i1++) {
                    double d6 = i1 * Math.PI * 2.0 / segs;
                    double d7 = (i1 + 1) * Math.PI * 2.0 / segs;
                    quadTint(
                        vertexconsumer,
                        mesh$frame.pose,
                        mesh$frame.x(x + Math.cos(d6) * d4),
                        mesh$frame.y(d2),
                        mesh$frame.z(z + Math.sin(d6) * d4),
                        k,
                        mesh$frame.x(x + Math.cos(d7) * d4),
                        mesh$frame.y(d2),
                        mesh$frame.z(z + Math.sin(d7) * d4),
                        k,
                        mesh$frame.x(x + Math.cos(d7) * d5),
                        mesh$frame.y(d3),
                        mesh$frame.z(z + Math.sin(d7) * d5),
                        l,
                        mesh$frame.x(x + Math.cos(d6) * d5),
                        mesh$frame.y(d3),
                        mesh$frame.z(z + Math.sin(d6) * d5),
                        l
                    );
                }
            }
        }
    }

    private static int bandTint(int zenith, int horizon, int nadir, float t) {
        return t < 0.5F ? Theme.lerp(zenith, horizon, t * 2.0F) : Theme.lerp(horizon, nadir, (t - 0.5F) * 2.0F);
    }

    public static int alpha(int rgb, float a) {
        int i = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
        return i << 24 | rgb & 16777215;
    }

    /** Scales the alpha already packed into {@code argb} without touching its colour channels. */
    private static int fade(int argb, float mul) {
        int i = Math.round((argb >>> 24) * Math.max(0.0F, mul));
        return Math.min(255, i) << 24 | argb & 16777215;
    }

    /** Nothing below one alpha step is visible, so callers can skip the geometry entirely. */
    private static boolean clear(int argb) {
        return (argb >>> 24) == 0;
    }

    public static void playerModel(
        WorldRenderContext ctx, Identifier skin, double x, double y, double z, float yawDeg, float scale, float alpha, Mesh.Pose pose, int rgbTint
    ) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && skin != null) {
            int i = alpha(rgbTint & 16777215, alpha);
            if (!clear(i)) {
                float f = 0.0625F * Math.max(0.2F, scale);
                float f1 = (float)Math.toRadians(yawDeg);
                float f2 = (float)Math.cos(f1);
                float f3 = (float)Math.sin(f1);
                VertexConsumer vertexconsumer = mesh$frame.buf(RenderLayers.entityTranslucentEmissive(skin));
                skinBox(mesh$frame, vertexconsumer, x, y, z, f2, f3, f, 0.0F, 18.0F, 0.0F, 8.0F, 12.0F, 4.0F, 16.0F, 16.0F, 8.0F, 12.0F, 4.0F, i);
                skinBox(mesh$frame, vertexconsumer, x, y, z, f2, f3, f, 0.0F, 28.0F, 0.0F, 8.0F, 8.0F, 8.0F, 0.0F, 0.0F, 8.0F, 8.0F, 8.0F, i);
                skinBox(mesh$frame, vertexconsumer, x, y, z, f2, f3, f, -2.0F, 6.0F, 0.0F, 4.0F, 12.0F, 4.0F, 0.0F, 16.0F, 4.0F, 12.0F, 4.0F, i);
                skinBox(mesh$frame, vertexconsumer, x, y, z, f2, f3, f, 2.0F, 6.0F, 0.0F, 4.0F, 12.0F, 4.0F, 16.0F, 48.0F, 4.0F, 12.0F, 4.0F, i);
                if (pose == Mesh.Pose.CRUCIFORM) {
                    skinBox(mesh$frame, vertexconsumer, x, y, z, f2, f3, f, -10.0F, 22.0F, 0.0F, 12.0F, 4.0F, 4.0F, 40.0F, 16.0F, 4.0F, 12.0F, 4.0F, i);
                    skinBox(mesh$frame, vertexconsumer, x, y, z, f2, f3, f, 10.0F, 22.0F, 0.0F, 12.0F, 4.0F, 4.0F, 32.0F, 48.0F, 4.0F, 12.0F, 4.0F, i);
                } else {
                    skinBox(mesh$frame, vertexconsumer, x, y, z, f2, f3, f, -6.0F, 18.0F, 0.0F, 4.0F, 12.0F, 4.0F, 40.0F, 16.0F, 4.0F, 12.0F, 4.0F, i);
                    skinBox(mesh$frame, vertexconsumer, x, y, z, f2, f3, f, 6.0F, 18.0F, 0.0F, 4.0F, 12.0F, 4.0F, 32.0F, 48.0F, 4.0F, 12.0F, 4.0F, i);
                }
            }
        }
    }

    public static void modelCube(
        WorldRenderContext ctx,
        Identifier tex,
        double x,
        double y,
        double z,
        float yawDeg,
        float scale,
        float cx,
        float cy,
        float cz,
        float w,
        float h,
        float d,
        float u,
        float v,
        float texW,
        float texH,
        int argb
    ) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && tex != null && w > 0.0F && h > 0.0F && d > 0.0F && !clear(argb)) {
            float f = 0.0625F * Math.max(0.05F, scale);
            float f1 = (float)Math.toRadians(yawDeg);
            VertexConsumer vertexconsumer = mesh$frame.buf(RenderLayers.entityTranslucentEmissive(tex));
            cubeFaces(
                mesh$frame,
                vertexconsumer,
                x,
                y,
                z,
                (float)Math.cos(f1),
                (float)Math.sin(f1),
                f,
                cx,
                cy,
                cz,
                w,
                h,
                d,
                u,
                v,
                w,
                h,
                d,
                1.0F / (texW <= 1.0F ? 64.0F : texW),
                1.0F / (texH <= 1.0F ? 64.0F : texH),
                argb
            );
        }
    }

    /**
     * One limb or the head of a player skin. Skins always use the vanilla box unwrap, so a texture
     * origin plus the box dimensions is enough to derive all six face rectangles. The unwrap size is
     * kept separate from the geometry size because the cruciform pose reuses the upright arm
     * rectangle on a box that lies flat.
     */
    private static void skinBox(
        Mesh.Frame f,
        VertexConsumer buf,
        double ox,
        double oy,
        double oz,
        float cos,
        float sin,
        float px,
        float cx,
        float cy,
        float cz,
        float w,
        float h,
        float d,
        float u,
        float v,
        float uw,
        float uh,
        float ud,
        int argb
    ) {
        cubeFaces(f, buf, ox, oy, oz, cos, sin, px, cx, cy, cz, w, h, d, u, v, uw, uh, ud, 0.015625F, 0.015625F, argb);
    }

    private static void cubeFaces(
        Mesh.Frame fr,
        VertexConsumer buf,
        double ox,
        double oy,
        double oz,
        float cos,
        float sin,
        float px,
        float cx,
        float cy,
        float cz,
        float w,
        float h,
        float d,
        float u,
        float v,
        float uw,
        float uh,
        float ud,
        float su,
        float sv,
        int argb
    ) {
        float f = w * 0.5F * px;
        float f1 = d * 0.5F * px;
        float f2 = cx * px;
        float f3 = cz * px;
        float f4 = fr.y((float)oy + (cy - h * 0.5F) * px);
        float f5 = fr.y((float)oy + (cy + h * 0.5F) * px);
        float f6 = -f + f2;
        float f7 = f + f2;
        float f8 = -f1 + f3;
        float f9 = f1 + f3;
        float f10 = fr.x(ox + f6 * cos - f8 * sin);
        float f11 = fr.z(oz + f6 * sin + f8 * cos);
        float f12 = fr.x(ox + f7 * cos - f8 * sin);
        float f13 = fr.z(oz + f7 * sin + f8 * cos);
        float f14 = fr.x(ox + f7 * cos - f9 * sin);
        float f15 = fr.z(oz + f7 * sin + f9 * cos);
        float f16 = fr.x(ox + f6 * cos - f9 * sin);
        float f17 = fr.z(oz + f6 * sin + f9 * cos);
        float f18 = u * su;
        float f19 = (u + ud) * su;
        float f20 = (u + ud + uw) * su;
        float f21 = (u + ud + uw + ud) * su;
        float f22 = (u + ud + uw + ud + uw) * su;
        float f23 = (u + ud + uw + uw) * su;
        float f24 = v * sv;
        float f25 = (v + ud) * sv;
        float f26 = (v + ud + uh) * sv;
        texFace(buf, fr.pose, f10, f4, f11, f12, f4, f13, f12, f5, f13, f10, f5, f11, f19, f26, f20, f25, argb);
        texFace(buf, fr.pose, f14, f4, f15, f16, f4, f17, f16, f5, f17, f14, f5, f15, f21, f26, f22, f25, argb);
        texFace(buf, fr.pose, f12, f4, f13, f14, f4, f15, f14, f5, f15, f12, f5, f13, f20, f26, f21, f25, argb);
        texFace(buf, fr.pose, f16, f4, f17, f10, f4, f11, f10, f5, f11, f16, f5, f17, f18, f26, f19, f25, argb);
        texFace(buf, fr.pose, f16, f5, f17, f14, f5, f15, f12, f5, f13, f10, f5, f11, f19, f24, f20, f25, argb);
        texFace(buf, fr.pose, f10, f4, f11, f12, f4, f13, f14, f4, f15, f16, f4, f17, f20, f24, f23, f25, argb);
    }

    private static void texFace(
        VertexConsumer b,
        Entry p,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4,
        float u0,
        float v1,
        float u1,
        float v0,
        int argb
    ) {
        texVert(b, p, x1, y1, z1, u0, v1, argb);
        texVert(b, p, x2, y2, z2, u1, v1, argb);
        texVert(b, p, x3, y3, z3, u1, v0, argb);
        texVert(b, p, x4, y4, z4, u0, v0, argb);
    }

    public static void billboard(WorldRenderContext ctx, Identifier tex, double x, double y, double z, float halfW, float halfH, int argb) {
        billboardUv(ctx, tex, x, y, z, halfW, halfH, 0.0F, 0.0F, 1.0F, 1.0F, argb);
    }

    public static void billboardUv(
        WorldRenderContext ctx, Identifier tex, double x, double y, double z, float halfW, float halfH, float u0, float v0, float u1, float v1, int argb
    ) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && halfW > 1.0E-4F && halfH > 1.0E-4F && !clear(argb)) {
            Quaternionf quaternionf = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
            float f = mesh$frame.x(x);
            float f1 = mesh$frame.y(y);
            float f2 = mesh$frame.z(z);
            Vector3f vector3f = CORNER.get();
            VertexConsumer vertexconsumer = mesh$frame.buf(RenderLayers.entityTranslucentEmissive(tex));
            texCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, -halfW, -halfH, u0, v1, argb);
            texCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, halfW, -halfH, u1, v1, argb);
            texCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, halfW, halfH, u1, v0, argb);
            texCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, -halfW, halfH, u0, v0, argb);
        }
    }

    private static void texCorner(
        VertexConsumer buf, Entry pose, Vector3f p, Quaternionf rot, float cx, float cy, float cz, float ox, float oy, float u, float v, int argb
    ) {
        p.set(ox, oy, 0.0F).rotate(rot);
        texVert(buf, pose, cx + p.x, cy + p.y, cz + p.z, u, v, argb);
    }

    public static void yawQuad(
        WorldRenderContext ctx,
        Identifier tex,
        double x,
        double y,
        double z,
        float yawDeg,
        float halfW,
        float halfH,
        float lean,
        float u0,
        float v0,
        float u1,
        float v1,
        int argb
    ) {
        yawQuad(ctx, tex, x, y, z, yawDeg, halfW, halfH, lean, u0, v0, u1, v1, argb, false);
    }

    public static void yawQuad(
        WorldRenderContext ctx,
        Identifier tex,
        double x,
        double y,
        double z,
        float yawDeg,
        float halfW,
        float halfH,
        float lean,
        float u0,
        float v0,
        float u1,
        float v1,
        int argb,
        boolean emissive
    ) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && halfW > 1.0E-4F && halfH > 1.0E-4F && !clear(argb)) {
            double d0 = Math.toRadians(yawDeg);
            float f = (float)Math.sin(d0);
            float f1 = (float)(-Math.cos(d0));
            float f2 = (float)Math.cos(d0);
            float f3 = (float)Math.sin(d0);
            float f4 = mesh$frame.x(x);
            float f5 = mesh$frame.y(y);
            float f6 = mesh$frame.z(z);
            float f7 = f4 - f2 * halfW + f * lean;
            float f8 = f6 - f3 * halfW + f1 * lean;
            float f9 = f4 + f2 * halfW + f * lean;
            float f10 = f6 + f3 * halfW + f1 * lean;
            float f11 = f4 - f2 * halfW - f * lean;
            float f12 = f6 - f3 * halfW - f1 * lean;
            float f13 = f4 + f2 * halfW - f * lean;
            float f14 = f6 + f3 * halfW - f1 * lean;
            float f15 = f5 - halfH;
            float f16 = f5 + halfH;
            VertexConsumer vertexconsumer = mesh$frame.buf(emissive ? RenderLayers.entityTranslucentEmissive(tex) : RenderLayers.entityTranslucent(tex));
            texVert(vertexconsumer, mesh$frame.pose, f7, f15, f8, u0, v1, argb);
            texVert(vertexconsumer, mesh$frame.pose, f9, f15, f10, u1, v1, argb);
            texVert(vertexconsumer, mesh$frame.pose, f13, f16, f14, u1, v0, argb);
            texVert(vertexconsumer, mesh$frame.pose, f11, f16, f12, u0, v0, argb);
        }
    }

    private static void texVert(VertexConsumer b, Entry p, float x, float y, float z, float u, float v, int argb) {
        b.vertex(p, x, y, z)
            .color(argb)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(15728880)
            .normal(p, 0.0F, 1.0F, 0.0F);
    }

    public static void softOrb(WorldRenderContext ctx, double x, double y, double z, float radius, int segs, int argb) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && radius > 1.0E-4F && segs >= 8 && !clear(argb)) {
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
            Quaternionf quaternionf = camera.getRotation();
            float f = mesh$frame.x(x);
            float f1 = mesh$frame.y(y);
            float f2 = mesh$frame.z(z);
            Vector3f vector3f = CORNER.get();
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.ghost());
            int i = Math.max(1, VisualQuality.orbLayers());
            int j = Math.max(8, Math.min(segs, VisualQuality.orbSegs()));

            for (int k = 0; k < i; k++) {
                float f3 = i == 1 ? 0.0F : (float)k / (i - 1);
                float f4 = radius * (1.0F - f3 * 0.45F);
                int l = fade(argb, 0.55F - f3 * 0.35F);
                if (!clear(l)) {
                    float f5 = 0.0F;
                    float f6 = 0.0F;
                    float f7 = 0.0F;

                    for (int i1 = 0; i1 <= j; i1++) {
                        double d0 = i1 * Math.PI * 2.0 / j;
                        vector3f.set((float)(Math.cos(d0) * f4), (float)(Math.sin(d0) * f4), 0.0F).rotate(quaternionf);
                        if (i1 > 0) {
                            quad(
                                vertexconsumer,
                                mesh$frame.pose,
                                f,
                                f1,
                                f2,
                                f + f5,
                                f1 + f6,
                                f2 + f7,
                                f + vector3f.x,
                                f1 + vector3f.y,
                                f2 + vector3f.z,
                                f,
                                f1,
                                f2,
                                l
                            );
                        }

                        f5 = vector3f.x;
                        f6 = vector3f.y;
                        f7 = vector3f.z;
                    }
                }
            }
        }
    }

    public static void softBillboard(WorldRenderContext ctx, double x, double y, double z, float half, int argb) {
        Mesh.Frame mesh$frame = frame(ctx);
        if (mesh$frame != null && half > 1.0E-4F && !clear(argb)) {
            Quaternionf quaternionf = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
            float f = mesh$frame.x(x);
            float f1 = mesh$frame.y(y);
            float f2 = mesh$frame.z(z);
            Vector3f vector3f = CORNER.get();
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.glow());
            flatQuad(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, half, argb);
            int i = fade(argb, 0.35F);
            if (!clear(i)) {
                flatQuad(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, half * 1.65F, i);
            }
        }
    }

    private static void flatQuad(
        VertexConsumer buf, Entry pose, Vector3f p, Quaternionf rot, float cx, float cy, float cz, float half, int argb
    ) {
        p.set(-half, -half, 0.0F).rotate(rot);
        float f = cx + p.x;
        float f1 = cy + p.y;
        float f2 = cz + p.z;
        p.set(half, -half, 0.0F).rotate(rot);
        float f3 = cx + p.x;
        float f4 = cy + p.y;
        float f5 = cz + p.z;
        p.set(half, half, 0.0F).rotate(rot);
        float f6 = cx + p.x;
        float f7 = cy + p.y;
        float f8 = cz + p.z;
        p.set(-half, half, 0.0F).rotate(rot);
        quad(buf, pose, f, f1, f2, f3, f4, f5, f6, f7, f8, cx + p.x, cy + p.y, cz + p.z, argb);
    }

    private static void quad(
        VertexConsumer b,
        Entry p,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4,
        int argb
    ) {
        b.vertex(p, x1, y1, z1).color(argb);
        b.vertex(p, x2, y2, z2).color(argb);
        b.vertex(p, x3, y3, z3).color(argb);
        b.vertex(p, x4, y4, z4).color(argb);
    }

    private static void quadTint(
        VertexConsumer b,
        Entry p,
        float x1,
        float y1,
        float z1,
        int c1,
        float x2,
        float y2,
        float z2,
        int c2,
        float x3,
        float y3,
        float z3,
        int c3,
        float x4,
        float y4,
        float z4,
        int c4
    ) {
        b.vertex(p, x1, y1, z1).color(c1);
        b.vertex(p, x2, y2, z2).color(c2);
        b.vertex(p, x3, y3, z3).color(c3);
        b.vertex(p, x4, y4, z4).color(c4);
    }

    private static void line(VertexConsumer b, Entry p, float x1, float y1, float z1, float x2, float y2, float z2, int argb, float w) {
        float f = x2 - x1;
        float f1 = y2 - y1;
        float f2 = z2 - z1;
        float f3 = (float)Math.sqrt(f * f + f1 * f1 + f2 * f2);
        float f4 = f3 > 1.0E-4F ? f / f3 : 0.0F;
        float f5 = f3 > 1.0E-4F ? f1 / f3 : 1.0F;
        float f6 = f3 > 1.0E-4F ? f2 / f3 : 0.0F;
        b.vertex(p, x1, y1, z1).color(argb).normal(p, f4, f5, f6).lineWidth(w);
        b.vertex(p, x2, y2, z2).color(argb).normal(p, f4, f5, f6).lineWidth(w);
    }

    private static Mesh.Frame frame(WorldRenderContext ctx) {
        MatrixStack matrixstack = ctx.matrices();
        VertexConsumerProvider vertexconsumerprovider = ctx.consumers();
        return matrixstack != null && vertexconsumerprovider != null
            ? FRAME.get().set(matrixstack.peek(), MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos(), vertexconsumerprovider)
            : null;
    }

    /**
     * Per-thread scratch for the camera-relative transform. Every entry point refreshes it from the
     * render context, so no draw method may hold one across a call into another draw method.
     */
    private static final class Frame {
        private Entry pose;
        private Vec3d cam;
        private VertexConsumerProvider src;

        Mesh.Frame set(Entry pose, Vec3d cam, VertexConsumerProvider src) {
            this.pose = pose;
            this.cam = cam;
            this.src = src;
            return this;
        }

        VertexConsumer buf(RenderLayer t) {
            return this.src.getBuffer(t);
        }

        float x(double v) {
            return (float)(v - this.cam.x);
        }

        float y(double v) {
            return (float)(v - this.cam.y);
        }

        float z(double v) {
            return (float)(v - this.cam.z);
        }
    }

    public static enum Pose {
        STAND,
        CRUCIFORM;
    }
}
