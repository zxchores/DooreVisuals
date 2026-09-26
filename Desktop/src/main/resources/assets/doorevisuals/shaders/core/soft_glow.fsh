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
    float pulse = doore_pulse(GameTime * 2800.0 + localPos.y * 2.0, 1.0, 0.28);
    float rim = 0.85 + 0.35 * sin(GameTime * 1600.0 - length(viewPos.xy) * 0.4);
    float bloom = 1.2 + color.a * 0.95;
    // Same reasoning as esp_glow: the boost easily exceeds one, so it is rolled off in linear light.
    vec3 boosted = doore_toLinear(color.rgb) * bloom * pulse * rim;
    vec3 rgb = doore_toSrgb(doore_tonemap(boosted));
    rgb += doore_dither(gl_FragCoord.xy);
    fragColor = vec4(rgb, color.a) * ColorModulator;
}
