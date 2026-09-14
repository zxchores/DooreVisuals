#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

in vec4 vertexColor;
in vec2 bandPos;

out vec4 fragColor;

// bandPos.x runs along the arc and already carries the client's scroll offset, bandPos.y goes from
// zero at the bottom edge of the curtain to one at the top.
void main() {
    if (vertexColor.a <= 0.002) {
        discard;
    }

    float height = clamp(bandPos.y, 0.0, 1.0);

    // Stretching the domain vertically turns the noise into the streaks a curtain is made of.
    float curtain = doore_fbmWarp(vec2(bandPos.x * 2.6, height * 0.55), 0.85);
    float band = smoothstep(0.34, 0.86, curtain);
    float fine = doore_fbm(vec2(bandPos.x * 9.0, height * 1.2));
    band *= 0.55 + 0.45 * fine;

    // Bright and dense at the base, dissolving upward, with the very bottom edge feathered.
    float fade = pow(1.0 - height, 1.5) * smoothstep(0.0, 0.22, height);
    float alpha = band * fade * vertexColor.a;
    if (alpha <= 0.003) {
        discard;
    }

    vec3 rgb = doore_toSrgb(doore_tonemap(doore_toLinear(vertexColor.rgb) * (0.9 + 1.5 * band)));
    rgb += doore_dither(gl_FragCoord.xy);
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
