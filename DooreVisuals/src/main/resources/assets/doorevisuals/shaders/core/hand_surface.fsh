#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

uniform sampler2D Sampler0;
// Акцент темы: текстура 1x1, которую клиент перезаливает при смене темы.
uniform sampler2D Sampler1;

in vec4 vertexColor;
in vec2 texCoord;
in vec3 localPos;
in vec3 viewPos;
in vec3 normal;

out vec4 fragColor;

// Стиль приходит дефайном пайплайна: 0 — Аура, 1 — Неон, 2 — Пульс, 3 — Контур.
#ifndef DOORE_STYLE
#define DOORE_STYLE 0
#endif

/** Переливающаяся палитра: оттенок течёт по кругу, поэтому стыков не видно. */
vec3 doore_iridescent(float phase) {
    return 0.5 + 0.5 * cos(6.2831853 * (phase + vec3(0.0, 0.3333333, 0.6666667)));
}

void main() {
    // Маска — текстура скина: перелив идёт по реальным пикселям руки, а не по кубоиду. Порог мягкий:
    // если разметка текстуры неожиданная, эффект всё равно останется видимым, а не пропадёт целиком.
    float maskRaw = texture(Sampler0, texCoord).a;
    if (maskRaw <= 0.02) {
        discard;
    }

    float mask = mix(1.0, smoothstep(0.02, 0.25, maskRaw), 0.65);

    vec3 viewDir = normalize(-viewPos);
    float edge = 1.0 - clamp(abs(dot(normalize(normal), viewDir)), 0.0, 1.0);

#if DOORE_STYLE == 1
    // Неон: частые быстрые волны и жёсткая светящаяся кромка.
    float speed = 1.55;
    float density = 9.0;
    float rimPower = 1.5;
    float base = 0.30;
    float amplitude = 0.55;
    float alphaScale = 0.62;
    float hueMix = 0.45;
#elif DOORE_STYLE == 2
    // Пульс: широкие медленные волны, дышащие целиком.
    float speed = 0.42;
    float density = 3.6;
    float rimPower = 2.6;
    float base = 0.26;
    float amplitude = 0.50;
    float alphaScale = 0.58;
    float hueMix = 0.60;
#elif DOORE_STYLE == 3
    // Контур: волн нет, только кромка силуэта в цвете темы.
    float speed = 0.0;
    float density = 0.0;
    float rimPower = 1.1;
    float base = 0.0;
    float amplitude = 0.0;
    float alphaScale = 0.72;
    float hueMix = 0.20;
#else
    // Аура: спокойные волны средней частоты.
    float speed = 0.80;
    float density = 5.6;
    float rimPower = 2.0;
    float base = 0.34;
    float amplitude = 0.62;
    float alphaScale = 0.60;
    float hueMix = 0.55;
#endif

    float t = GameTime * speed;

    // Три волновых потока: вдоль руки, поперёк и медленный «вихрь». Их интерференция и даёт перелив —
    // гребни то складываются, то гасят друг друга, поэтому узор течёт сам и никогда не повторяется.
    float along = texCoord.y * density * 6.0 - t * 2.2;
    float across = texCoord.x * density * 2.4 + localPos.y * 3.1;
    float swirl = 0.5 + 0.5 * sin(localPos.x * 2.4 + localPos.z * 1.8 + t * 0.45);
    float flow = along + 0.55 * sin(across - t * 0.9) + 0.35 * swirl;
    float bands = 0.5 + 0.5 * sin(flow);
    float shimmer = 0.5 + 0.5 * sin(flow * 0.5 + across * 0.85 + t * 0.55);

    vec3 accent = texture(Sampler1, vec2(0.5, 0.5)).rgb;
    vec3 rainbow = doore_iridescent(0.16 * flow + 0.10 * shimmer + 0.05 * localPos.y);
    // Оттенки держатся вокруг акцента темы, но продолжают переливаться.
    vec3 palette = mix(accent, rainbow, hueMix);

    float rim = pow(clamp(edge, 0.0, 1.0), rimPower);
    float gain = (base + amplitude * bands) * (0.45 + 0.55 * rim);
    gain *= 0.55 + 0.45 * mask;

#if DOORE_STYLE == 3
    // У контура заливки нет: светятся только рёбра.
    gain = rim * 0.85 * (0.55 + 0.45 * mask);
#endif

    if (gain <= 0.004) {
        discard;
    }

    // Линейный свет, компрессия и дизеринг — те же, что у остальных эффектов клиента.
    vec3 tone = doore_toLinear(palette) * (1.15 + gain * 1.25);
    vec3 rgb = doore_toSrgb(doore_tonemap(tone));
    rgb += doore_dither(gl_FragCoord.xy);

    fragColor = vec4(rgb, clamp(gain * alphaScale, 0.0, 0.78)) * ColorModulator;
}
