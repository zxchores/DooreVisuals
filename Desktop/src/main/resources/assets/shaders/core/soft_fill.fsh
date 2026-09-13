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
    float soft = doore_softDistance(viewPos);
    float breath = doore_pulse(GameTime * 1900.0 + localPos.y * 1.2, 1.0, 0.1);
    float grain = 0.96 + 0.04 * doore_noise(localPos.xz * 6.0 + GameTime * 40.0);
    fragColor = vec4(color.rgb * breath * grain, color.a * soft * 0.94) * ColorModulator;
}
