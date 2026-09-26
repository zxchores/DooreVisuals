#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

in vec4 vertexColor;
in vec3 viewPos;
in vec3 localPos;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    if (color.a <= 0.001) {
        discard;
    }
    float dist = length(viewPos);
    float fade = clamp(1.15 - dist * 0.01, 0.5, 1.0);
    // Boosting in linear light and rolling the result off keeps a saturated glow saturated instead of
    // letting the brightest channel clip and drag the colour toward white.
    vec3 rgb = doore_toLinear(color.rgb) * (1.15 + color.a * 0.35);
    rgb = doore_toSrgb(doore_tonemap(rgb));
    rgb += doore_dither(gl_FragCoord.xy);
    fragColor = vec4(rgb, color.a * fade) * ColorModulator;
}
