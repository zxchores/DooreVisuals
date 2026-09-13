#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 p = texCoord0 * 2.0 - 1.0;
    float d = length(p);
    if (d > 1.0) {
        discard;
    }
    float glow = exp(-d * d * 2.6);
    float core = exp(-d * d * 11.0);
    float a = (glow * 0.70 + core * 0.55) * vertexColor.a;
    if (a < 0.008) {
        discard;
    }
    vec3 rgb = mix(vertexColor.rgb, vec3(1.0), core * 0.58) * (0.82 + glow * 0.85);
    fragColor = vec4(rgb, a) * ColorModulator;
}
