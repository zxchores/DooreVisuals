#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec4 vertexColor;
in vec3 viewPos;
in vec3 localPos;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    if (color.a <= 0.001) {
        discard;
    }

    vec3 dir = normalize(localPos);
    float height = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    float haze = pow(1.0 - abs(dir.y), 3.2);
    float scatter = haze * (0.18 + 0.12 * height);
    vec3 rgb = color.rgb + vec3(0.16, 0.10, 0.05) * scatter;
    rgb *= 0.92 + 0.10 * height;
    float alpha = color.a * (0.78 + 0.22 * height);
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
