package dev.doorevisuals.draw;

import java.util.List;
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
    private static final ThreadLocal<Vec3d[]> RIBBON_SMOOTH = ThreadLocal.withInitial(() -> new Vec3d[256]);
    private static final ThreadLocal<Vector3f> ORB_CORNER = ThreadLocal.withInitial(Vector3f::new);
    private static final ThreadLocal<float[]> LINK_PTS = ThreadLocal.withInitial(() -> new float[96]);

    private Mesh() {
    }

    public static void cylinder(WorldRenderContext ctx, double x, double y, double z, double r, float h, int segs, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && segs >= 8) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            float[] afloat = rgba(argb);
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
                    afloat
                );
            }
        }
    }

    public static void disc(WorldRenderContext ctx, double x, double y, double z, double inner, double outer, int segs, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && segs >= 6 && !(outer <= inner)) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            float[] afloat = rgba(argb);
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
                    afloat
                );
            }
        }
    }

    public static void shell(WorldRenderContext ctx, double x, double y, double z, double r, float h, int layers, int segs, int argb, RenderLayer type) {
        volumeCapsule(ctx, x, y, z, r, h, layers, segs, argb, type);
    }

    public static void capsule(WorldRenderContext ctx, double x, double y, double z, double r, float h, int segs, int stacks, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && segs >= 8 && !(r <= 1.0E-4) && !(h <= 1.0E-4)) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            float[] afloat = rgba(argb);
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
                        afloat
                    );
                }
            }

            int j = Math.max(4, stacks);
            hemisphere(mesh$frame, vertexconsumer, x, f, z, d0, false, segs, j, afloat);
            hemisphere(mesh$frame, vertexconsumer, x, f1, z, d0, true, segs, j, afloat);
        }
    }

    public static void volumeCapsule(WorldRenderContext ctx, double x, double y, double z, double r, float h, int layers, int segs, int argb, RenderLayer type) {
        int i = Math.max(2, layers);
        float f = (argb >> 24 & 0xFF) / 255.0F;
        int j = argb & 16777215;

        for (int k = 0; k < i; k++) {
            float f1 = i == 1 ? 0.0F : (float)k / (i - 1);
            double d0 = r * (0.62 + 0.48 * f1);
            float f2 = h * (0.9F + 0.1F * (1.0F - f1));
            float f3 = f * (0.55F - f1 * 0.42F) * (0.55F + 0.45F / i);
            capsule(ctx, x, y - (f2 - h) * 0.5, z, d0, f2, Math.max(12, segs - k), 8, alpha(j, f3), type);
        }
    }

    public static void ellipsoid(
        WorldRenderContext ctx, double x, double y, double z, double rx, double ry, double rz, int segs, int stacks, int argb, RenderLayer type
    ) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && segs >= 8 && stacks >= 4) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            float[] afloat = rgba(argb);
            double d0 = y + ry;

            for (int i = 0; i < stacks; i++) {
                double d1 = Math.PI * i / stacks;
                double d2 = Math.PI * (i + 1) / stacks;

                for (int j = 0; j < segs; j++) {
                    double d3 = j * Math.PI * 2.0 / segs;
                    double d4 = (j + 1) * Math.PI * 2.0 / segs;
                    quad(
                        vertexconsumer,
                        mesh$frame.pose,
                        mesh$frame.x(x + rx * Math.sin(d1) * Math.cos(d3)),
                        mesh$frame.y(d0 + ry * Math.cos(d1)),
                        mesh$frame.z(z + rz * Math.sin(d1) * Math.sin(d3)),
                        mesh$frame.x(x + rx * Math.sin(d1) * Math.cos(d4)),
                        mesh$frame.y(d0 + ry * Math.cos(d1)),
                        mesh$frame.z(z + rz * Math.sin(d1) * Math.sin(d4)),
                        mesh$frame.x(x + rx * Math.sin(d2) * Math.cos(d4)),
                        mesh$frame.y(d0 + ry * Math.cos(d2)),
                        mesh$frame.z(z + rz * Math.sin(d2) * Math.sin(d4)),
                        mesh$frame.x(x + rx * Math.sin(d2) * Math.cos(d3)),
                        mesh$frame.y(d0 + ry * Math.cos(d2)),
                        mesh$frame.z(z + rz * Math.sin(d2) * Math.sin(d3)),
                        afloat
                    );
                }
            }
        }
    }

    public static void volumeEllipsoid(
        WorldRenderContext ctx, double x, double y, double z, double rx, double ry, double rz, int layers, int segs, int argb, RenderLayer type
    ) {
        int i = Math.max(2, layers);
        float f = (argb >> 24 & 0xFF) / 255.0F;
        int j = argb & 16777215;

        for (int k = 0; k < i; k++) {
            float f1 = i == 1 ? 0.0F : (float)k / (i - 1);
            float f2 = f * (0.5F - f1 * 0.38F) * (0.5F + 0.5F / i);
            ellipsoid(ctx, x, y, z, rx * (0.7 + 0.4 * f1), ry * (0.75 + 0.35 * f1), rz * (0.7 + 0.4 * f1), Math.max(14, segs - k), 10, alpha(j, f2), type);
        }
    }

    public static void torus(WorldRenderContext ctx, double x, double y, double z, double major, double minor, int segs, int tube, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && segs >= 8 && tube >= 6 && !(major <= 0.0) && !(minor <= 0.0)) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            float[] afloat = rgba(argb);
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
                        afloat
                    );
                }
            }
        }
    }

    public static void cone(WorldRenderContext ctx, double x, double y, double z, double rBottom, double rTop, float h, int segs, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && segs >= 6 && !(h <= 0.0F)) {
            VertexConsumer vertexconsumer = mesh$frame.buf(type);
            float[] afloat = rgba(argb);
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
                    afloat
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
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && !(half <= 1.0E-4F) && !(loop <= 1.0E-4F) && !(tube <= 1.0E-4F)) {
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
                    double d29 = 0.0;
                    double d30 = 1.0;
                    double d31 = 0.0;
                    d1 = d30 * ty;
                    nx = d29 - d1 * tx;
                    ny = d30 - d1 * ty;
                    nz = d31 - d1 * tz;
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
                float[] afloat1 = rgba(argb);
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
                                afloat1
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

    private static void hemisphere(Mesh.Frame f, VertexConsumer buf, double cx, double cy, double cz, double r, boolean top, int segs, int stacks, float[] c) {
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
                    c
                );
            }
        }
    }

    public static void helix(WorldRenderContext ctx, double x, double y, double z, float h, double r, double phase, int strands, int segs, int argb) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && segs >= 4) {
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.linesTx());
            float[] afloat = rgba(argb);
            int i = Math.max(1, strands);

            for (int j = 0; j < i; j++) {
                double d0 = j * Math.PI * 2.0 / i;

                for (int k = 0; k < segs; k++) {
                    double d1 = (double)k / segs;
                    double d2 = (double)(k + 1) / segs;
                    double d3 = phase + d0 + d1 * Math.PI * 4.5;
                    double d4 = phase + d0 + d2 * Math.PI * 4.5;
                    line(
                        vertexconsumer,
                        mesh$frame.pose,
                        mesh$frame.x(x + Math.cos(d3) * r),
                        mesh$frame.y(y + h * d1),
                        mesh$frame.z(z + Math.sin(d3) * r),
                        mesh$frame.x(x + Math.cos(d4) * r),
                        mesh$frame.y(y + h * d2),
                        mesh$frame.z(z + Math.sin(d4) * r),
                        afloat[0],
                        afloat[1],
                        afloat[2],
                        afloat[3] * (0.4F + 0.6F * (float)Math.sin(d1 * Math.PI)),
                        2.1F
                    );
                }
            }
        }
    }

    public static void segment(WorldRenderContext ctx, double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
        segment(ctx, x1, y1, z1, x2, y2, z2, argb, 2.6F);
    }

    public static void segment(WorldRenderContext ctx, double x1, double y1, double z1, double x2, double y2, double z2, int argb, float width) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null) {
            float[] afloat = rgba(argb);
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.linesTx());
            line(
                vertexconsumer,
                mesh$frame.pose,
                mesh$frame.x(x1),
                mesh$frame.y(y1),
                mesh$frame.z(z1),
                mesh$frame.x(x2),
                mesh$frame.y(y2),
                mesh$frame.z(z2),
                afloat[0],
                afloat[1],
                afloat[2],
                afloat[3],
                width
            );
        }
    }

    public static void strip(WorldRenderContext ctx, double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
        segment(ctx, x1, y1, z1, x2, y2, z2, alpha(argb, 0.35F), 5.2F);
        segment(ctx, x1, y1, z1, x2, y2, z2, alpha(argb, 0.95F), 2.0F);
    }

    public static void ring(WorldRenderContext ctx, double x, double y, double z, double r, int segs, int argb) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && segs >= 3) {
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.lines());
            float[] afloat = rgba(argb);
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
                    afloat[0],
                    afloat[1],
                    afloat[2],
                    afloat[3],
                    2.4F
                );
            }
        }
    }

    public static void box(WorldRenderContext ctx, Box box, int fill, int edge) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null) {
            faces(mesh$frame, box, rgba(fill), Types.fill());
            wire(mesh$frame, box, rgba(edge), 2.0F, Types.lines());
        }
    }

    public static void cage(WorldRenderContext ctx, Box box, int fill, int edge, float bloom) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null) {
            float f = Math.max(0.25F, bloom);
            Box boxx = box.expand(0.045 * f);
            faces(mesh$frame, boxx, rgba(alpha(fill, 0.16F * f)), Types.glow());
            faces(mesh$frame, box, rgba(fill), Types.holo());
            wire(mesh$frame, box, rgba(alpha(edge, 0.32F * f)), 5.4F + 1.6F * f, Types.linesTx());
            wire(mesh$frame, box, rgba(edge), 1.85F, Types.linesTx());
        }
    }

    public static void scanPlane(WorldRenderContext ctx, Box box, float t, int argb) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null) {
            float f = Math.max(0.0F, Math.min(1.0F, t));
            float f1 = mesh$frame.y(box.minY + (box.maxY - box.minY) * f);
            float f2 = mesh$frame.x(box.minX);
            float f3 = mesh$frame.z(box.minZ);
            float f4 = mesh$frame.x(box.maxX);
            float f5 = mesh$frame.z(box.maxZ);
            float[] afloat = rgba(argb);
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.glow());
            quad(vertexconsumer, mesh$frame.pose, f2, f1, f3, f4, f1, f3, f4, f1, f5, f2, f1, f5, afloat);
            float[] afloat1 = new float[]{afloat[0], afloat[1], afloat[2], Math.min(1.0F, afloat[3] * 1.35F)};
            VertexConsumer vertexconsumer1 = mesh$frame.buf(Types.linesTx());
            line(vertexconsumer1, mesh$frame.pose, f2, f1, f3, f4, f1, f3, afloat1[0], afloat1[1], afloat1[2], afloat1[3], 2.2F);
            line(vertexconsumer1, mesh$frame.pose, f4, f1, f3, f4, f1, f5, afloat1[0], afloat1[1], afloat1[2], afloat1[3], 2.2F);
            line(vertexconsumer1, mesh$frame.pose, f4, f1, f5, f2, f1, f5, afloat1[0], afloat1[1], afloat1[2], afloat1[3], 2.2F);
            line(vertexconsumer1, mesh$frame.pose, f2, f1, f5, f2, f1, f3, afloat1[0], afloat1[1], afloat1[2], afloat1[3], 2.2F);
        }
    }

    public static void comet(WorldRenderContext ctx, List<Vec3d> points, float halfWidth, int argb) {
        if (points != null && points.size() >= 2 && !(halfWidth <= 1.0E-4F)) {
            glowTrail(ctx, points, halfWidth, argb);
            Vec3d vec3d = points.get(points.size() - 1);
            orb(ctx, vec3d.x, vec3d.y, vec3d.z, halfWidth * 2.35F, alpha(argb, 0.42F));
            orb(ctx, vec3d.x, vec3d.y, vec3d.z, halfWidth * 1.12F, argb);
            float[] afloat = rgba(argb);
            orb(ctx, vec3d.x, vec3d.y, vec3d.z, halfWidth * 0.36F, alpha(-1, Math.min(1.0F, afloat[3] * 1.05F)));
        }
    }

    public static void glowTrail(WorldRenderContext ctx, List<Vec3d> points, float halfWidth, int argb) {
        if (points != null && points.size() >= 2 && !(halfWidth <= 1.0E-4F)) {
            Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
            if (mesh$frame != null) {
                VertexConsumer vertexconsumer = mesh$frame.buf(Types.glow());
                float[] afloat = rgba(argb);
                int i = points.size();
                Vec3d vec3d = mesh$frame.cam;

                for (int j = 0; j < i - 1; j++) {
                    Vec3d vec3d1 = points.get(j);
                    Vec3d vec3d2 = points.get(j + 1);
                    double d0 = vec3d2.x - vec3d1.x;
                    double d1 = vec3d2.y - vec3d1.y;
                    double d2 = vec3d2.z - vec3d1.z;
                    double d3 = (vec3d1.x + vec3d2.x) * 0.5;
                    double d4 = (vec3d1.y + vec3d2.y) * 0.5;
                    double d5 = (vec3d1.z + vec3d2.z) * 0.5;
                    double d6 = vec3d.x - d3;
                    double d7 = vec3d.y - d4;
                    double d8 = vec3d.z - d5;
                    double d9 = d1 * d8 - d2 * d7;
                    double d10 = d2 * d6 - d0 * d8;
                    double d11 = d0 * d7 - d1 * d6;
                    double d12 = Math.sqrt(d9 * d9 + d10 * d10 + d11 * d11);
                    if (d12 < 1.0E-5) {
                        d9 = -d2;
                        d10 = 0.0;
                        d11 = d0;
                        d12 = Math.sqrt(d9 * d9 + d0 * d0);
                        if (d12 < 1.0E-5) {
                            continue;
                        }
                    }

                    d9 /= d12;
                    d10 /= d12;
                    d11 /= d12;
                    float f = (float)j / (i - 1);
                    float f1 = (float)(j + 1) / (i - 1);
                    float f2 = f * f * (3.0F - 2.0F * f);
                    float f3 = f1 * f1 * (3.0F - 2.0F * f1);
                    float f4 = halfWidth * (0.16F + 0.84F * f2);
                    float f5 = halfWidth * (0.16F + 0.84F * f3);
                    float f6 = 0.035F + 0.965F * f2;
                    float f7 = 0.035F + 0.965F * f3;
                    trailQuad(vertexconsumer, mesh$frame, vec3d1, vec3d2, d9, d10, d11, f4 * 2.15F, f5 * 2.15F, afloat, f6 * 0.28F, f7 * 0.28F);
                    trailQuad(vertexconsumer, mesh$frame, vec3d1, vec3d2, d9, d10, d11, f4, f5, afloat, f6, f7);
                }
            }
        }
    }

    public static void orb(WorldRenderContext ctx, double x, double y, double z, float radius, int argb) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && !(radius <= 1.0E-4F)) {
            Quaternionf quaternionf = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
            float f = mesh$frame.x(x);
            float f1 = mesh$frame.y(y);
            float f2 = mesh$frame.z(z);
            float[] afloat = rgba(argb);
            Vector3f vector3f = ORB_CORNER.get();
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.orb());
            orbCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, -radius, -radius, 0.0F, 1.0F, afloat);
            orbCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, radius, -radius, 1.0F, 1.0F, afloat);
            orbCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, radius, radius, 1.0F, 0.0F, afloat);
            orbCorner(vertexconsumer, mesh$frame.pose, vector3f, quaternionf, f, f1, f2, -radius, radius, 0.0F, 0.0F, afloat);
        }
    }

    private static void orbCorner(
        VertexConsumer buf, Entry pose, Vector3f p, Quaternionf rot, float cx, float cy, float cz, float ox, float oy, float u, float v, float[] c
    ) {
        p.set(ox, oy, 0.0F).rotate(rot);
        buf.vertex(pose, cx + p.x, cy + p.y, cz + p.z).texture(u, v).color(c[0], c[1], c[2], c[3]);
    }

    private static void trailQuad(
        VertexConsumer buf, Mesh.Frame f, Vec3d a, Vec3d b, double rx, double ry, double rz, float w0, float w1, float[] base, float a0, float a1
    ) {
        float[] afloat = new float[]{base[0], base[1], base[2], base[3] * a0};
        float[] afloat1 = new float[]{base[0], base[1], base[2], base[3] * a1};
        float fx = f.x(a.x);
        float f1 = f.y(a.y);
        float f2 = f.z(a.z);
        float f3 = f.x(b.x);
        float f4 = f.y(b.y);
        float f5 = f.z(b.z);
        float f6 = (float)(rx * w0);
        float f7 = (float)(ry * w0);
        float f8 = (float)(rz * w0);
        float f9 = (float)(rx * w1);
        float f10 = (float)(ry * w1);
        float f11 = (float)(rz * w1);
        quadTint(
            buf,
            f.pose,
            fx - f6,
            f1 - f7,
            f2 - f8,
            afloat,
            fx + f6,
            f1 + f7,
            f2 + f8,
            afloat,
            f3 + f9,
            f4 + f10,
            f5 + f11,
            afloat1,
            f3 - f9,
            f4 - f10,
            f5 - f11,
            afloat1
        );
    }

    private static void faces(Mesh.Frame f, Box box, float[] c, RenderLayer type) {
        float fx = f.x(box.minX);
        float f1 = f.y(box.minY);
        float f2 = f.z(box.minZ);
        float f3 = f.x(box.maxX);
        float f4 = f.y(box.maxY);
        float f5 = f.z(box.maxZ);
        VertexConsumer vertexconsumer = f.buf(type);
        quad(vertexconsumer, f.pose, fx, f1, f2, f3, f1, f2, f3, f1, f5, fx, f1, f5, c);
        quad(vertexconsumer, f.pose, fx, f4, f2, fx, f4, f5, f3, f4, f5, f3, f4, f2, c);
        quad(vertexconsumer, f.pose, fx, f1, f2, fx, f4, f2, f3, f4, f2, f3, f1, f2, c);
        quad(vertexconsumer, f.pose, fx, f1, f5, f3, f1, f5, f3, f4, f5, fx, f4, f5, c);
        quad(vertexconsumer, f.pose, fx, f1, f2, fx, f1, f5, fx, f4, f5, fx, f4, f2, c);
        quad(vertexconsumer, f.pose, f3, f1, f2, f3, f4, f2, f3, f4, f5, f3, f1, f5, c);
    }

    private static void wire(Mesh.Frame f, Box box, float[] c, float width, RenderLayer type) {
        float fx = f.x(box.minX);
        float f1 = f.y(box.minY);
        float f2 = f.z(box.minZ);
        float f3 = f.x(box.maxX);
        float f4 = f.y(box.maxY);
        float f5 = f.z(box.maxZ);
        VertexConsumer vertexconsumer = f.buf(type);
        line(vertexconsumer, f.pose, fx, f1, f2, f3, f1, f2, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, fx, f1, f5, f3, f1, f5, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, fx, f4, f2, f3, f4, f2, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, fx, f4, f5, f3, f4, f5, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, fx, f1, f2, fx, f1, f5, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, f3, f1, f2, f3, f1, f5, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, fx, f4, f2, fx, f4, f5, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, f3, f4, f2, f3, f4, f5, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, fx, f1, f2, fx, f4, f2, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, f3, f1, f2, f3, f4, f2, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, fx, f1, f5, fx, f4, f5, c[0], c[1], c[2], c[3], width);
        line(vertexconsumer, f.pose, f3, f1, f5, f3, f4, f5, c[0], c[1], c[2], c[3], width);
    }

    public static void ribbon(WorldRenderContext ctx, List<Vec3d> points, float halfWidth, int argb) {
        ribbon(ctx, points, halfWidth, argb, 0.88F, true, false, true, 0);
    }

    public static void ribbon(WorldRenderContext ctx, List<Vec3d> points, float halfWidth, int argb, float yLift, boolean grow) {
        ribbon(ctx, points, halfWidth, argb, yLift, grow, false, true, 0);
    }

    public static void ribbon(
        WorldRenderContext ctx, List<Vec3d> points, float halfWidth, int argb, float yLift, boolean grow, boolean vertical, boolean wave, int fadeHead
    ) {
        if (points != null && points.size() >= 2 && !(halfWidth <= 1.0E-4F)) {
            Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
            if (mesh$frame != null) {
                int i = points.size();
                int j = Math.max(i, (i - 1) * 6 + 1);
                Vec3d[] avec3d = RIBBON_SMOOTH.get();
                if (avec3d.length < j) {
                    avec3d = new Vec3d[j + 32];
                    RIBBON_SMOOTH.set(avec3d);
                }

                int k = 0;

                for (int l = 0; l < i - 1; l++) {
                    Vec3d vec3d = points.get(Math.max(0, l - 1));
                    Vec3d vec3d1 = points.get(l);
                    Vec3d vec3d2 = points.get(l + 1);
                    Vec3d vec3d3 = points.get(Math.min(i - 1, l + 2));
                    int i1 = l == i - 2 ? 6 : 5;

                    for (int j1 = 0; j1 < i1; j1++) {
                        float f = (float)j1 / i1;
                        avec3d[k++] = catmull(vec3d, vec3d1, vec3d2, vec3d3, f);
                    }
                }

                avec3d[k++] = points.get(i - 1);
                if (k >= 2) {
                    VertexConsumer vertexconsumer = mesh$frame.buf(Types.glow());
                    float[] afloat3 = rgba(argb);
                    int l1 = Math.max(0, fadeHead);

                    for (int i2 = 0; i2 < k - 1; i2++) {
                        Vec3d vec3d4 = avec3d[i2];
                        Vec3d vec3d5 = avec3d[i2 + 1];
                        double d2 = vec3d5.x - vec3d4.x;
                        double d0 = vec3d5.z - vec3d4.z;
                        double d1 = Math.sqrt(d2 * d2 + d0 * d0);
                        if (!(d1 < 1.0E-5)) {
                            float f1 = (float)(-d0 / d1);
                            float f2 = (float)(d2 / d1);
                            float f3 = (float)i2 / Math.max(1, k - 1);
                            float f4 = (float)(i2 + 1) / Math.max(1, k - 1);
                            float f5 = grow ? halfWidth * (0.35F + 0.75F * f3) : halfWidth;
                            float f6 = grow ? halfWidth * (0.35F + 0.75F * f4) : halfWidth;
                            float f7 = wave ? (float)Math.sin(f3 * Math.PI * 2.2) * halfWidth * 0.18F : 0.0F;
                            float f8 = wave ? (float)Math.sin(f4 * Math.PI * 2.2) * halfWidth * 0.18F : 0.0F;
                            float f9 = (float)vec3d4.y + yLift + f7;
                            float f10 = (float)vec3d5.y + yLift + f8;
                            float f11 = grow ? 0.08F + 0.72F * f3 : 0.35F + 0.55F * (float)Math.sin(f3 * Math.PI);
                            float f12 = grow ? 0.08F + 0.72F * f4 : 0.35F + 0.55F * (float)Math.sin(f4 * Math.PI);
                            int k1 = k - 2 - i2;
                            if (k1 < l1) {
                                float f13 = (float)k1 / Math.max(1, l1);
                                f11 *= f13 * 0.35F;
                                f12 *= f13 * 0.35F;
                            }

                            float[] afloat4 = new float[]{afloat3[0], afloat3[1], afloat3[2], afloat3[3] * f11};
                            float[] afloat = new float[]{afloat3[0], afloat3[1], afloat3[2], afloat3[3] * f12};
                            float f14 = mesh$frame.x(vec3d4.x);
                            float f15 = mesh$frame.z(vec3d4.z);
                            float f16 = mesh$frame.x(vec3d5.x);
                            float f17 = mesh$frame.z(vec3d5.z);
                            if (vertical) {
                                quadTint(
                                    vertexconsumer,
                                    mesh$frame.pose,
                                    f14,
                                    mesh$frame.y(f9 - f5 * 1.55F),
                                    f15,
                                    afloat4,
                                    f14,
                                    mesh$frame.y(f9 + f5 * 1.55F),
                                    f15,
                                    afloat4,
                                    f16,
                                    mesh$frame.y(f10 + f6 * 1.55F),
                                    f17,
                                    afloat,
                                    f16,
                                    mesh$frame.y(f10 - f6 * 1.55F),
                                    f17,
                                    afloat
                                );
                                float[] afloat1 = new float[]{afloat3[0], afloat3[1], afloat3[2], Math.min(1.0F, afloat4[3] * 1.35F)};
                                float[] afloat2 = new float[]{afloat3[0], afloat3[1], afloat3[2], Math.min(1.0F, afloat[3] * 1.35F)};
                                quadTint(
                                    vertexconsumer,
                                    mesh$frame.pose,
                                    f14,
                                    mesh$frame.y(f9 - f5),
                                    f15,
                                    afloat1,
                                    f14,
                                    mesh$frame.y(f9 + f5),
                                    f15,
                                    afloat1,
                                    f16,
                                    mesh$frame.y(f10 + f6),
                                    f17,
                                    afloat2,
                                    f16,
                                    mesh$frame.y(f10 - f6),
                                    f17,
                                    afloat2
                                );
                            } else {
                                quadTint(
                                    vertexconsumer,
                                    mesh$frame.pose,
                                    f14 - f1 * f5 * 1.55F,
                                    mesh$frame.y(f9),
                                    f15 - f2 * f5 * 1.55F,
                                    afloat4,
                                    f14 + f1 * f5 * 1.55F,
                                    mesh$frame.y(f9),
                                    f15 + f2 * f5 * 1.55F,
                                    afloat4,
                                    f16 + f1 * f6 * 1.55F,
                                    mesh$frame.y(f10),
                                    f17 + f2 * f6 * 1.55F,
                                    afloat,
                                    f16 - f1 * f6 * 1.55F,
                                    mesh$frame.y(f10),
                                    f17 - f2 * f6 * 1.55F,
                                    afloat
                                );
                                float[] afloat5 = new float[]{afloat3[0], afloat3[1], afloat3[2], Math.min(1.0F, afloat4[3] * 1.35F)};
                                float[] afloat6 = new float[]{afloat3[0], afloat3[1], afloat3[2], Math.min(1.0F, afloat[3] * 1.35F)};
                                quadTint(
                                    vertexconsumer,
                                    mesh$frame.pose,
                                    f14 - f1 * f5,
                                    mesh$frame.y(f9 + 0.01F),
                                    f15 - f2 * f5,
                                    afloat5,
                                    f14 + f1 * f5,
                                    mesh$frame.y(f9 + 0.01F),
                                    f15 + f2 * f5,
                                    afloat5,
                                    f16 + f1 * f6,
                                    mesh$frame.y(f10 + 0.01F),
                                    f17 + f2 * f6,
                                    afloat6,
                                    f16 - f1 * f6,
                                    mesh$frame.y(f10 + 0.01F),
                                    f17 - f2 * f6,
                                    afloat6
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    private static Vec3d catmull(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
        float f = t * t;
        float f1 = f * t;
        double d0 = 0.5
            * (
                2.0 * p1.x
                    + (-p0.x + p2.x) * t
                    + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * f
                    + (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * f1
            );
        double d1 = 0.5
            * (
                2.0 * p1.y
                    + (-p0.y + p2.y) * t
                    + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * f
                    + (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * f1
            );
        double d2 = 0.5
            * (
                2.0 * p1.z
                    + (-p0.z + p2.z) * t
                    + (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * f
                    + (-p0.z + 3.0 * p1.z - 3.0 * p2.z + p3.z) * f1
            );
        return new Vec3d(d0, d1, d2);
    }

    private static void quadTint(
        VertexConsumer b,
        Entry p,
        float x1,
        float y1,
        float z1,
        float[] c1,
        float x2,
        float y2,
        float z2,
        float[] c2,
        float x3,
        float y3,
        float z3,
        float[] c3,
        float x4,
        float y4,
        float z4,
        float[] c4
    ) {
        b.vertex(p, x1, y1, z1).color(c1[0], c1[1], c1[2], c1[3]);
        b.vertex(p, x2, y2, z2).color(c2[0], c2[1], c2[2], c2[3]);
        b.vertex(p, x3, y3, z3).color(c3[0], c3[1], c3[2], c3[3]);
        b.vertex(p, x4, y4, z4).color(c4[0], c4[1], c4[2], c4[3]);
    }

    public static void dome(WorldRenderContext ctx, double x, double y, double z, double r, int segs, int stacks, int argb, RenderLayer type) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && !(r <= 1.0E-4) && segs >= 8) {
            hemisphere(mesh$frame, mesh$frame.buf(type), x, y, z, r, true, segs, Math.max(4, stacks), rgba(argb));
        }
    }

    public static int alpha(int rgb, float a) {
        int i = Math.round(Math.max(0.04F, Math.min(1.0F, a)) * 255.0F);
        return i << 24 | rgb & 16777215;
    }

    public static void playerModel(WorldRenderContext ctx, Identifier skin, double x, double y, double z, float yawDeg, float scale, float alpha) {
        playerModel(ctx, skin, x, y, z, yawDeg, scale, alpha, Mesh.Pose.STAND);
    }

    public static void playerModel(
        WorldRenderContext ctx, Identifier skin, double x, double y, double z, float yawDeg, float scale, float alpha, Mesh.Pose pose
    ) {
        playerModel(ctx, skin, x, y, z, yawDeg, scale, alpha, pose, 16777215);
    }

    public static void playerModel(
        WorldRenderContext ctx, Identifier skin, double x, double y, double z, float yawDeg, float scale, float alpha, Mesh.Pose pose, int rgbTint
    ) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && skin != null && !(alpha < 0.02F)) {
            float f = 0.0625F * Math.max(0.2F, scale);
            float f1 = (float)Math.toRadians(yawDeg);
            float f2 = (float)Math.cos(f1);
            float f3 = (float)Math.sin(f1);
            int i = alpha(rgbTint & 16777215, alpha);
            VertexConsumer vertexconsumer = mesh$frame.buf(RenderLayers.entityTranslucentEmissive(skin));
            skinPart(
                mesh$frame,
                vertexconsumer,
                x,
                y,
                z,
                f2,
                f3,
                f,
                0.0F,
                18.0F,
                0.0F,
                8.0F,
                12.0F,
                4.0F,
                20.0F,
                20.0F,
                28.0F,
                32.0F,
                32.0F,
                20.0F,
                40.0F,
                32.0F,
                16.0F,
                20.0F,
                20.0F,
                32.0F,
                28.0F,
                20.0F,
                32.0F,
                32.0F,
                20.0F,
                16.0F,
                28.0F,
                20.0F,
                28.0F,
                16.0F,
                36.0F,
                20.0F,
                i
            );
            if (pose == Mesh.Pose.CRUCIFORM) {
                skinPart(
                    mesh$frame,
                    vertexconsumer,
                    x,
                    y,
                    z,
                    f2,
                    f3,
                    f,
                    -10.0F,
                    22.0F,
                    0.0F,
                    12.0F,
                    4.0F,
                    4.0F,
                    44.0F,
                    20.0F,
                    48.0F,
                    32.0F,
                    52.0F,
                    20.0F,
                    56.0F,
                    32.0F,
                    40.0F,
                    20.0F,
                    44.0F,
                    32.0F,
                    48.0F,
                    20.0F,
                    52.0F,
                    32.0F,
                    44.0F,
                    16.0F,
                    48.0F,
                    20.0F,
                    48.0F,
                    16.0F,
                    52.0F,
                    20.0F,
                    i
                );
                skinPart(
                    mesh$frame,
                    vertexconsumer,
                    x,
                    y,
                    z,
                    f2,
                    f3,
                    f,
                    10.0F,
                    22.0F,
                    0.0F,
                    12.0F,
                    4.0F,
                    4.0F,
                    36.0F,
                    52.0F,
                    40.0F,
                    64.0F,
                    44.0F,
                    52.0F,
                    48.0F,
                    64.0F,
                    32.0F,
                    52.0F,
                    36.0F,
                    64.0F,
                    40.0F,
                    52.0F,
                    44.0F,
                    64.0F,
                    36.0F,
                    48.0F,
                    40.0F,
                    52.0F,
                    40.0F,
                    48.0F,
                    44.0F,
                    52.0F,
                    i
                );
            } else {
                skinPart(
                    mesh$frame,
                    vertexconsumer,
                    x,
                    y,
                    z,
                    f2,
                    f3,
                    f,
                    -6.0F,
                    18.0F,
                    0.0F,
                    4.0F,
                    12.0F,
                    4.0F,
                    44.0F,
                    20.0F,
                    48.0F,
                    32.0F,
                    52.0F,
                    20.0F,
                    56.0F,
                    32.0F,
                    40.0F,
                    20.0F,
                    44.0F,
                    32.0F,
                    48.0F,
                    20.0F,
                    52.0F,
                    32.0F,
                    44.0F,
                    16.0F,
                    48.0F,
                    20.0F,
                    48.0F,
                    16.0F,
                    52.0F,
                    20.0F,
                    i
                );
                skinPart(
                    mesh$frame,
                    vertexconsumer,
                    x,
                    y,
                    z,
                    f2,
                    f3,
                    f,
                    6.0F,
                    18.0F,
                    0.0F,
                    4.0F,
                    12.0F,
                    4.0F,
                    36.0F,
                    52.0F,
                    40.0F,
                    64.0F,
                    44.0F,
                    52.0F,
                    48.0F,
                    64.0F,
                    32.0F,
                    52.0F,
                    36.0F,
                    64.0F,
                    40.0F,
                    52.0F,
                    44.0F,
                    64.0F,
                    36.0F,
                    48.0F,
                    40.0F,
                    52.0F,
                    40.0F,
                    48.0F,
                    44.0F,
                    52.0F,
                    i
                );
            }

            skinPart(
                mesh$frame,
                vertexconsumer,
                x,
                y,
                z,
                f2,
                f3,
                f,
                -2.0F,
                6.0F,
                0.0F,
                4.0F,
                12.0F,
                4.0F,
                4.0F,
                20.0F,
                8.0F,
                32.0F,
                12.0F,
                20.0F,
                16.0F,
                32.0F,
                0.0F,
                20.0F,
                4.0F,
                32.0F,
                8.0F,
                20.0F,
                12.0F,
                32.0F,
                4.0F,
                16.0F,
                8.0F,
                20.0F,
                8.0F,
                16.0F,
                12.0F,
                20.0F,
                i
            );
            skinPart(
                mesh$frame,
                vertexconsumer,
                x,
                y,
                z,
                f2,
                f3,
                f,
                2.0F,
                6.0F,
                0.0F,
                4.0F,
                12.0F,
                4.0F,
                20.0F,
                52.0F,
                24.0F,
                64.0F,
                28.0F,
                52.0F,
                32.0F,
                64.0F,
                16.0F,
                52.0F,
                20.0F,
                64.0F,
                24.0F,
                52.0F,
                28.0F,
                64.0F,
                20.0F,
                48.0F,
                24.0F,
                52.0F,
                24.0F,
                48.0F,
                28.0F,
                52.0F,
                i
            );
            skinPart(
                mesh$frame,
                vertexconsumer,
                x,
                y,
                z,
                f2,
                f3,
                f,
                0.0F,
                28.0F,
                0.0F,
                8.0F,
                8.0F,
                8.0F,
                8.0F,
                8.0F,
                16.0F,
                16.0F,
                24.0F,
                8.0F,
                32.0F,
                16.0F,
                0.0F,
                8.0F,
                8.0F,
                16.0F,
                16.0F,
                8.0F,
                24.0F,
                16.0F,
                8.0F,
                0.0F,
                16.0F,
                8.0F,
                16.0F,
                0.0F,
                24.0F,
                8.0F,
                i
            );
        }
    }

    private static void skinPart(
        Mesh.Frame f,
        VertexConsumer buf,
        double ox,
        double oy,
        double oz,
        float cy,
        float sy,
        float px,
        float lx,
        float ly,
        float lz,
        float w,
        float h,
        float d,
        float fu0,
        float fv0,
        float fu1,
        float fv1,
        float bu0,
        float bv0,
        float bu1,
        float bv1,
        float ru0,
        float rv0,
        float ru1,
        float rv1,
        float lu0,
        float lv0,
        float lu1,
        float lv1,
        float tu0,
        float tv0,
        float tu1,
        float tv1,
        float du0,
        float dv0,
        float du1,
        float dv1,
        int tint
    ) {
        float fx = w * 0.5F * px;
        float f1 = h * 0.5F * px;
        float f2 = d * 0.5F * px;
        float f3 = lx * px;
        float f4 = lz * px;
        float f5 = (float)oy + (ly - h * 0.5F) * px;
        float f6 = (float)oy + (ly + h * 0.5F) * px;
        float[][] afloat = new float[][]{{-fx + f3, -f2 + f4}, {fx + f3, -f2 + f4}, {fx + f3, f2 + f4}, {-fx + f3, f2 + f4}};
        float[] afloat1 = new float[4];
        float[] afloat2 = new float[4];

        for (int i = 0; i < 4; i++) {
            afloat1[i] = f.x(ox + afloat[i][0] * cy - afloat[i][1] * sy);
            afloat2[i] = f.z(oz + afloat[i][0] * sy + afloat[i][1] * cy);
        }

        float f9 = f.y(f5);
        float f7 = f.y(f6);
        float f8 = 0.015625F;
        texFace(
            buf,
            f.pose,
            afloat1[0],
            f9,
            afloat2[0],
            afloat1[1],
            f9,
            afloat2[1],
            afloat1[1],
            f7,
            afloat2[1],
            afloat1[0],
            f7,
            afloat2[0],
            fu0 * f8,
            fv1 * f8,
            fu1 * f8,
            fv0 * f8,
            tint
        );
        texFace(
            buf,
            f.pose,
            afloat1[2],
            f9,
            afloat2[2],
            afloat1[3],
            f9,
            afloat2[3],
            afloat1[3],
            f7,
            afloat2[3],
            afloat1[2],
            f7,
            afloat2[2],
            bu0 * f8,
            bv1 * f8,
            bu1 * f8,
            bv0 * f8,
            tint
        );
        texFace(
            buf,
            f.pose,
            afloat1[1],
            f9,
            afloat2[1],
            afloat1[2],
            f9,
            afloat2[2],
            afloat1[2],
            f7,
            afloat2[2],
            afloat1[1],
            f7,
            afloat2[1],
            ru0 * f8,
            rv1 * f8,
            ru1 * f8,
            rv0 * f8,
            tint
        );
        texFace(
            buf,
            f.pose,
            afloat1[3],
            f9,
            afloat2[3],
            afloat1[0],
            f9,
            afloat2[0],
            afloat1[0],
            f7,
            afloat2[0],
            afloat1[3],
            f7,
            afloat2[3],
            lu0 * f8,
            lv1 * f8,
            lu1 * f8,
            lv0 * f8,
            tint
        );
        texFace(
            buf,
            f.pose,
            afloat1[3],
            f7,
            afloat2[3],
            afloat1[0],
            f7,
            afloat2[0],
            afloat1[1],
            f7,
            afloat2[1],
            afloat1[2],
            f7,
            afloat2[2],
            tu0 * f8,
            tv1 * f8,
            tu1 * f8,
            tv0 * f8,
            tint
        );
        texFace(
            buf,
            f.pose,
            afloat1[0],
            f9,
            afloat2[0],
            afloat1[3],
            f9,
            afloat2[3],
            afloat1[2],
            f9,
            afloat2[2],
            afloat1[1],
            f9,
            afloat2[1],
            du0 * f8,
            dv1 * f8,
            du1 * f8,
            dv0 * f8,
            tint
        );
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
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && !(halfW <= 1.0E-4F) && !(halfH <= 1.0E-4F)) {
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
            float[] afloat = rgba(argb);
            int i = packArgb(afloat[0], afloat[1], afloat[2], afloat[3]);
            VertexConsumer vertexconsumer = mesh$frame.buf(emissive ? RenderLayers.entityTranslucentEmissive(tex) : RenderLayers.entityTranslucent(tex));
            texVert(vertexconsumer, mesh$frame.pose, f7, f15, f8, u0, v1, i);
            texVert(vertexconsumer, mesh$frame.pose, f9, f15, f10, u1, v1, i);
            texVert(vertexconsumer, mesh$frame.pose, f13, f16, f14, u1, v0, i);
            texVert(vertexconsumer, mesh$frame.pose, f11, f16, f12, u0, v0, i);
        }
    }

    public static void yawSlab(
        WorldRenderContext ctx,
        Identifier tex,
        double x,
        double y,
        double z,
        float yawDeg,
        float halfW,
        float halfH,
        float halfDepth,
        float lean,
        float u0,
        float v0,
        float u1,
        float v1,
        int argb
    ) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && !(halfW <= 1.0E-4F) && !(halfH <= 1.0E-4F)) {
            float f = Math.max(0.008F, halfDepth);
            double d0 = Math.toRadians(yawDeg);
            float f1 = (float)Math.sin(d0);
            float f2 = (float)(-Math.cos(d0));
            float f3 = (float)Math.cos(d0);
            float f4 = (float)Math.sin(d0);
            float f5 = mesh$frame.x(x);
            float f6 = mesh$frame.y(y);
            float f7 = mesh$frame.z(z);
            float f8 = f6 - halfH;
            float f9 = f6 + halfH;
            float[][] afloat = new float[2][12];

            for (int i = 0; i < 2; i++) {
                float f10 = i == 0 ? -f : f;
                float f11 = lean + f10;
                afloat[i][0] = f5 - f3 * halfW + f1 * f11;
                afloat[i][1] = f8;
                afloat[i][2] = f7 - f4 * halfW + f2 * f11;
                afloat[i][3] = f5 + f3 * halfW + f1 * f11;
                afloat[i][4] = f8;
                afloat[i][5] = f7 + f4 * halfW + f2 * f11;
                afloat[i][6] = f5 + f3 * halfW + f1 * f10;
                afloat[i][7] = f9;
                afloat[i][8] = f7 + f4 * halfW + f2 * f10;
                afloat[i][9] = f5 - f3 * halfW + f1 * f10;
                afloat[i][10] = f9;
                afloat[i][11] = f7 - f4 * halfW + f2 * f10;
            }

            float[] afloat1 = rgba(argb);
            int k = packArgb(afloat1[0], afloat1[1], afloat1[2], afloat1[3]);
            int l = packArgb(afloat1[0] * 0.82F, afloat1[1] * 0.82F, afloat1[2] * 0.82F, afloat1[3] * 0.95F);
            int j = packArgb(afloat1[0] * 0.55F, afloat1[1] * 0.55F, afloat1[2] * 0.55F, afloat1[3] * 0.9F);
            VertexConsumer vertexconsumer = mesh$frame.buf(RenderLayers.entityTranslucent(tex));
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][0], afloat[0][1], afloat[0][2], u0, v1, k);
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][3], afloat[0][4], afloat[0][5], u1, v1, k);
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][6], afloat[0][7], afloat[0][8], u1, v0, k);
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][9], afloat[0][10], afloat[0][11], u0, v0, k);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][3], afloat[1][4], afloat[1][5], u1, v1, l);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][0], afloat[1][1], afloat[1][2], u0, v1, l);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][9], afloat[1][10], afloat[1][11], u0, v0, l);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][6], afloat[1][7], afloat[1][8], u1, v0, l);
            float f12 = (u0 + u1) * 0.5F;
            float f13 = (v0 + v1) * 0.5F;
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][0], afloat[0][1], afloat[0][2], u0, v1, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][0], afloat[1][1], afloat[1][2], f12, v1, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][9], afloat[1][10], afloat[1][11], f12, v0, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][9], afloat[0][10], afloat[0][11], u0, v0, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][3], afloat[1][4], afloat[1][5], f12, v1, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][3], afloat[0][4], afloat[0][5], u1, v1, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][6], afloat[0][7], afloat[0][8], u1, v0, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][6], afloat[1][7], afloat[1][8], f12, v0, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][0], afloat[0][1], afloat[0][2], u0, f13, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][3], afloat[0][4], afloat[0][5], u1, f13, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][3], afloat[1][4], afloat[1][5], u1, v1, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][0], afloat[1][1], afloat[1][2], u0, v1, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][9], afloat[0][10], afloat[0][11], u0, v0, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][9], afloat[1][10], afloat[1][11], u0, f13, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[1][6], afloat[1][7], afloat[1][8], u1, f13, j);
            texVert(vertexconsumer, mesh$frame.pose, afloat[0][6], afloat[0][7], afloat[0][8], u1, v0, j);
        }
    }

    private static int packArgb(float r, float g, float b, float a) {
        return (int)(Math.min(1.0F, Math.max(0.0F, a)) * 255.0F) << 24
            | (int)(Math.min(1.0F, Math.max(0.0F, r)) * 255.0F) << 16
            | (int)(Math.min(1.0F, Math.max(0.0F, g)) * 255.0F) << 8
            | (int)(Math.min(1.0F, Math.max(0.0F, b)) * 255.0F);
    }

    public static void billboardUv(
        WorldRenderContext ctx, Identifier tex, double x, double y, double z, float halfW, float halfH, float u0, float v0, float u1, float v1, int argb
    ) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && !(halfW <= 1.0E-4F) && !(halfH <= 1.0E-4F)) {
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
            Quaternionf quaternionf = camera.getRotation();
            float f = mesh$frame.x(x);
            float f1 = mesh$frame.y(y);
            float f2 = mesh$frame.z(z);
            Vector3f vector3f = new Vector3f(-halfW, -halfH, 0.0F).rotate(quaternionf);
            Vector3f vector3f1 = new Vector3f(halfW, -halfH, 0.0F).rotate(quaternionf);
            Vector3f vector3f2 = new Vector3f(halfW, halfH, 0.0F).rotate(quaternionf);
            Vector3f vector3f3 = new Vector3f(-halfW, halfH, 0.0F).rotate(quaternionf);
            float[] afloat = rgba(argb);
            int i = (int)(afloat[3] * 255.0F) << 24 | (int)(afloat[0] * 255.0F) << 16 | (int)(afloat[1] * 255.0F) << 8 | (int)(afloat[2] * 255.0F);
            VertexConsumer vertexconsumer = mesh$frame.buf(RenderLayers.entityTranslucentEmissive(tex));
            texVert(vertexconsumer, mesh$frame.pose, f + vector3f.x, f1 + vector3f.y, f2 + vector3f.z, u0, v1, i);
            texVert(vertexconsumer, mesh$frame.pose, f + vector3f1.x, f1 + vector3f1.y, f2 + vector3f1.z, u1, v1, i);
            texVert(vertexconsumer, mesh$frame.pose, f + vector3f2.x, f1 + vector3f2.y, f2 + vector3f2.z, u1, v0, i);
            texVert(vertexconsumer, mesh$frame.pose, f + vector3f3.x, f1 + vector3f3.y, f2 + vector3f3.z, u0, v0, i);
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
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && !(radius <= 1.0E-4F) && segs >= 8) {
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
            Quaternionf quaternionf = camera.getRotation();
            float f = mesh$frame.x(x);
            float f1 = mesh$frame.y(y);
            float f2 = mesh$frame.z(z);
            float[] afloat = rgba(argb);
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.ghost());
            int i = Math.max(1, VisualQuality.orbLayers());
            int j = Math.max(8, Math.min(segs, VisualQuality.orbSegs()));

            for (int k = 0; k < i; k++) {
                float f3 = i == 1 ? 0.0F : (float)k / (i - 1);
                float f4 = radius * (1.0F - f3 * 0.45F);
                float[] afloat1 = new float[]{afloat[0], afloat[1], afloat[2], afloat[3] * (0.55F - f3 * 0.35F)};
                Vector3f vector3f = null;

                for (int l = 0; l <= j; l++) {
                    double d0 = l * Math.PI * 2.0 / j;
                    Vector3f vector3f1 = new Vector3f((float)(Math.cos(d0) * f4), (float)(Math.sin(d0) * f4), 0.0F).rotate(quaternionf);
                    if (vector3f != null) {
                        quad(
                            vertexconsumer,
                            mesh$frame.pose,
                            f,
                            f1,
                            f2,
                            f + vector3f.x,
                            f1 + vector3f.y,
                            f2 + vector3f.z,
                            f + vector3f1.x,
                            f1 + vector3f1.y,
                            f2 + vector3f1.z,
                            f,
                            f1,
                            f2,
                            afloat1
                        );
                    }

                    vector3f = vector3f1;
                }
            }
        }
    }

    public static void softGlowBillboard(WorldRenderContext ctx, double x, double y, double z, float half, int argb) {
        if (VisualQuality.inFrustumDist(x, y, z) && VisualQuality.takeParticle()) {
            billboard(ctx, Sprites.SOFT_GLOW_ATLAS, x, y, z, half, half, argb);
            if (VisualQuality.level() == VisualQuality.Level.HIGH) {
                softBillboard(ctx, x, y, z, half * 0.8F, alpha(argb, 0.35F));
            }
        }
    }

    public static void softBillboard(WorldRenderContext ctx, double x, double y, double z, float half, int argb) {
        Mesh.Frame mesh$frame = Mesh.Frame.of(ctx);
        if (mesh$frame != null && !(half <= 1.0E-4F)) {
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
            Quaternionf quaternionf = camera.getRotation();
            float f = mesh$frame.x(x);
            float f1 = mesh$frame.y(y);
            float f2 = mesh$frame.z(z);
            Vector3f vector3f = new Vector3f(-half, -half, 0.0F).rotate(quaternionf);
            Vector3f vector3f1 = new Vector3f(half, -half, 0.0F).rotate(quaternionf);
            Vector3f vector3f2 = new Vector3f(half, half, 0.0F).rotate(quaternionf);
            Vector3f vector3f3 = new Vector3f(-half, half, 0.0F).rotate(quaternionf);
            float[] afloat = rgba(argb);
            VertexConsumer vertexconsumer = mesh$frame.buf(Types.glow());
            quad(
                vertexconsumer,
                mesh$frame.pose,
                f + vector3f.x,
                f1 + vector3f.y,
                f2 + vector3f.z,
                f + vector3f1.x,
                f1 + vector3f1.y,
                f2 + vector3f1.z,
                f + vector3f2.x,
                f1 + vector3f2.y,
                f2 + vector3f2.z,
                f + vector3f3.x,
                f1 + vector3f3.y,
                f2 + vector3f3.z,
                afloat
            );
            float f3 = half * 1.65F;
            float[] afloat1 = new float[]{afloat[0], afloat[1], afloat[2], afloat[3] * 0.35F};
            Vector3f vector3f4 = new Vector3f(-f3, -f3, 0.0F).rotate(quaternionf);
            Vector3f vector3f5 = new Vector3f(f3, -f3, 0.0F).rotate(quaternionf);
            Vector3f vector3f6 = new Vector3f(f3, f3, 0.0F).rotate(quaternionf);
            Vector3f vector3f7 = new Vector3f(-f3, f3, 0.0F).rotate(quaternionf);
            quad(
                vertexconsumer,
                mesh$frame.pose,
                f + vector3f4.x,
                f1 + vector3f4.y,
                f2 + vector3f4.z,
                f + vector3f5.x,
                f1 + vector3f5.y,
                f2 + vector3f5.z,
                f + vector3f6.x,
                f1 + vector3f6.y,
                f2 + vector3f6.z,
                f + vector3f7.x,
                f1 + vector3f7.y,
                f2 + vector3f7.z,
                afloat1
            );
        }
    }

    private static void edge(VertexConsumer b, Entry p, float x1, float y1, float z1, float x2, float y2, float z2, float[] c) {
        line(b, p, x1, y1, z1, x2, y2, z2, c[0], c[1], c[2], c[3], 2.0F);
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
        float[] c
    ) {
        b.vertex(p, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        b.vertex(p, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
        b.vertex(p, x3, y3, z3).color(c[0], c[1], c[2], c[3]);
        b.vertex(p, x4, y4, z4).color(c[0], c[1], c[2], c[3]);
    }

    private static void line(
        VertexConsumer b, Entry p, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float bl, float a, float w
    ) {
        float f = x2 - x1;
        float f1 = y2 - y1;
        float f2 = z2 - z1;
        float f3 = (float)Math.sqrt(f * f + f1 * f1 + f2 * f2);
        float f4 = f3 > 1.0E-4F ? f / f3 : 0.0F;
        float f5 = f3 > 1.0E-4F ? f1 / f3 : 1.0F;
        float f6 = f3 > 1.0E-4F ? f2 / f3 : 0.0F;
        b.vertex(p, x1, y1, z1).color(r, g, bl, a).normal(p, f4, f5, f6).lineWidth(w);
        b.vertex(p, x2, y2, z2).color(r, g, bl, a).normal(p, f4, f5, f6).lineWidth(w);
    }

    private static float[] rgba(int argb) {
        return new float[]{(argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, Math.max(0.04F, (argb >> 24 & 0xFF) / 255.0F)};
    }

    private record Frame(Entry pose, Vec3d cam, VertexConsumerProvider src) {
        static Mesh.Frame of(WorldRenderContext ctx) {
            MatrixStack matrixstack = ctx.matrices();
            VertexConsumerProvider vertexconsumerprovider = ctx.consumers();
            return matrixstack != null && vertexconsumerprovider != null
                ? new Mesh.Frame(matrixstack.peek(), MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos(), vertexconsumerprovider)
                : null;
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
