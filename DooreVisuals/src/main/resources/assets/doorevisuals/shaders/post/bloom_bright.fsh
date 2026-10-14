#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BrightConfig {
    float Threshold;
    float Knee;
};

in vec2 texCoord;

out vec4 fragColor;

// Keeps only what is bright enough to bleed light, so the sky, lava and the client's own glow
// effects bloom while ordinary terrain does not.
void main() {
    vec3 color = texture(InSampler, texCoord).rgb;
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));

    // Soft knee: the bloom fades in over a band instead of switching on at a hard threshold.
    float weight = smoothstep(Threshold, Threshold + max(Knee, 0.001), luma);
    fragColor = vec4(color * weight, 1.0);
}
