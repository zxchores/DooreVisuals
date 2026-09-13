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
    float t = GameTime * 4200.0 + localPos.y * 5.5 + length(localPos.xz) * 3.0;
    float wave = 0.5 + 0.5 * sin(t);
    float bolt = pow(max(0.0, sin(t * 2.7 + doore_fbm(localPos.xz * 8.0) * 6.0)), 16.0);
    float crackle = doore_fbm(localPos.xz * 10.0 + vec2(GameTime * 120.0, localPos.y * 3.0));
    vec3 tint = color.rgb * vec3(1.1 + wave * 0.35, 0.95 + crackle * 0.25, 1.25 - wave * 0.3);
    vec3 rgb = tint * (1.15 + wave * 0.55) + tint * bolt * 1.4 + tint * crackle * 0.35;
    fragColor = vec4(rgb, color.a * (0.7 + wave * 0.3)) * ColorModulator;
}
