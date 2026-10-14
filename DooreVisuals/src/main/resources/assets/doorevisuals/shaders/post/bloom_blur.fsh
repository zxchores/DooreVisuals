#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurConfig {
    vec2 BlurDir;
    float Radius;
};

in vec2 texCoord;

out vec4 fragColor;

// Fixed nine-tap gaussian: the loop bound stays constant so the shader compiles on every driver,
// while Radius stretches how far the taps reach and therefore how wide the glow spreads.
void main() {
    vec2 step = (1.0 / InSize) * BlurDir;
    float spread = max(Radius, 0.25) / 4.0;

    vec4 sum = vec4(0.0);
    float total = 0.0;

    for (int i = -4; i <= 4; i++) {
        float fi = float(i);
        float weight = exp(-(fi * fi) / 8.0);
        sum += texture(InSampler, texCoord + step * fi * spread) * weight;
        total += weight;
    }

    fragColor = sum / total;
}
