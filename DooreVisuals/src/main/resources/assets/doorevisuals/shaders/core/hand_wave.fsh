#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord;
in vec3 viewPos;
in vec3 normal;
flat in vec2 ctl;
flat in vec3 secondary;

out vec4 fragColor;

// 0: Waves, 1: Ripples, 2: Pulse, 3: Outline
#ifndef DOORE_WAVE_STYLE
#define DOORE_WAVE_STYLE 0
#endif

const float TAU = 6.2831853;

void main() {
    // The skin is the mask, so nothing is drawn where the arm model has no pixels (an empty sleeve layer).
    float maskRaw = texture(Sampler0, texCoord).a;
    if (maskRaw <= 0.02) {
        discard;
    }

    vec3 c1 = vertexColor.rgb;
    vec3 c2 = secondary;
    float userAlpha = vertexColor.a;
    float speed = max(0.05, ctl.x);
    float scale = max(0.2, ctl.y);

    // The phases come from the game clock (GameTime is 1 per 20 minutes), each wave at its own rate.
    float tt = GameTime * 1200.0 * speed;
    float pA = tt * 1.90;
    float pB = tt * 1.30;
    float pC = tt * 0.80;
    float pD = tt * 1.20;

    vec3 n = normalize(normal);
    vec3 viewDir = normalize(-viewPos);
    float rim = pow(1.0 - clamp(abs(dot(n, viewDir)), 0.0, 1.0), 2.2);

    // Two directions across the arm; the hand moves rigidly with the camera, so the waves slide over it as it moves.
    // The size slider makes the waves bigger, so it divides the frequency. At 1.0 two or three soft bands lie along the hand.
    float d1 = viewPos.x * 0.82 + viewPos.y * 0.57 + viewPos.z * 0.35;
    float d2 = viewPos.x * -0.46 + viewPos.y * 0.88 + viewPos.z * 0.9;

#if DOORE_WAVE_STYLE == 1
    // Ripples: the same big swell with a finer, water-like ripple running on top of it.
    float f = 15.0 / scale;
    float swell = sin(d1 * f + pA + 0.9 * sin(d2 * f * 0.7 + pC));
    float ripple = sin(d1 * f * 2.6 - pB * 1.6 + 1.8 * doore_noise(vec2(d2 * f * 0.8, pB * 0.3)));
    float wave = 0.5 + 0.5 * (swell * 0.7 + ripple * 0.3);
#elif DOORE_WAVE_STYLE == 2
    // Pulse: very broad waves that breathe as a whole.
    float f = 11.0 / scale;
    float w1 = sin(d1 * f + pA + 0.8 * sin(d2 * f * 0.8 + pC));
    float w2 = sin(d2 * f * 0.9 - pB + 1.7);
    float wave = 0.5 + 0.5 * (w1 * 0.65 + w2 * 0.35);
    wave *= 0.78 + 0.22 * sin(pD);
#else
    // Waves (and the base of the outline): big smooth swells, each one bending the next.
    float f = 13.0 / scale;
    float w1 = sin(d1 * f + pA + 0.9 * sin(d2 * f * 0.7 + pC));
    float w2 = sin(d2 * f * 0.8 - pB + 1.7 + 0.6 * sin(d1 * f * 0.5 + pA));
    float wave = 0.5 + 0.5 * (w1 * 0.65 + w2 * 0.35);
#endif

    // The waves are painted over the hand with ordinary alpha blending, so the theme colour is really seen on the skin:
    // the troughs of the swell are nearly clear, the crests carry most of the colour.
    float body = smoothstep(0.22, 0.78, wave);
    float top = pow(clamp(wave, 0.0, 1.0), 3.0);

    // Iridescence inside the theme: the shade slides between the deep primary, the secondary and a pale tint as the swell
    // passes, and a second, slower wave of the same kind shifts where each shade lies.
    float sheen = 0.5 + 0.5 * sin(wave * 4.6 + pB * 0.8 + d2 * 6.0 / scale);
    vec3 deep = c1 * 0.85;
    vec3 tint = mix(c2, vec3(1.0), 0.55);
    vec3 rgb = mix(deep, c2, body);
    rgb = mix(rgb, mix(c1, tint, sheen), 0.35 * body + 0.15);
    rgb = mix(rgb, tint, top * 0.5);

    float a = 0.05 + 0.72 * body + 0.18 * top;
    a += rim * 0.30;

#if DOORE_WAVE_STYLE == 3
    // Outline: the glow of the silhouette with a soft swell inside.
    a = rim * 0.90 + 0.22 * body;
    rgb = mix(c1, tint, rim * 0.6 + top * 0.3);
#endif

    a *= userAlpha * (0.55 + 0.45 * smoothstep(0.02, 0.25, maskRaw));
    if (a <= 0.004) {
        discard;
    }

    rgb += doore_dither(gl_FragCoord.xy);
    fragColor = vec4(rgb, clamp(a, 0.0, 0.92)) * ColorModulator;
}
