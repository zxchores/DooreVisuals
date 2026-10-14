#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

in vec4 vertexColor;
in vec3 viewPos;
in float phaseCoord;
in float shaderSpeed;
out vec4 fragColor;

vec3 hueColor(float h) {
    return clamp(abs(fract(h + vec3(0.0, 0.666667, 0.333333)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
}

void main() {
    if (vertexColor.a <= 0.002) discard;
    float coord = phaseCoord;
    float sweep = 0.5 + 0.5 * sin(coord * 2.8 - GameTime * 3400.0 * shaderSpeed);
    float glint = pow(max(0.0, 1.0 - abs(sweep - 0.5) * 3.2), 10.0);
    vec3 rainbow = hueColor(fract(coord * 0.16 - GameTime * 0.035 * shaderSpeed));
    vec3 rgb = mix(vertexColor.rgb, rainbow, 0.22 + 0.48 * sweep);
    rgb = mix(rgb, vec3(1.0), glint * 0.48);
    float depth = clamp(1.12 - length(viewPos) * 0.012, 0.42, 1.0);
    float alpha = vertexColor.a * (0.72 + sweep * 0.2 + glint * 0.28) * depth;
    rgb = doore_toSrgb(doore_tonemap(doore_toLinear(rgb) * (1.12 + glint * 0.5)));
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
