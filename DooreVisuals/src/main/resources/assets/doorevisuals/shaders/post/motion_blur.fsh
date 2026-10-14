#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform MotionConfig {
    vec2 BlurDir;
    float Samples;
    float Pad;
};

in vec2 texCoord;

out vec4 fragColor;

// Camera motion blur: the scene is averaged along BlurDir (a screen-space vector in uv units, already scaled by
// the camera's angular speed and the chosen exposure on the CPU). The loop bound is constant so the shader compiles
// everywhere; Samples only decides how many of the taps are used.
void main() {
    int n = int(clamp(Samples, 2.0, 32.0));
    vec4 sum = vec4(0.0);
    float total = 0.0;

    for (int i = 0; i < 32; i++) {
        if (i >= n) {
            break;
        }
        float t = (float(i) + 0.5) / float(n) - 0.5;
        // Tent weights keep the sharp centre sample dominant, so the blur reads as a trail and not as a smear.
        float w = 1.0 - abs(t) * 0.9;
        vec2 uv = clamp(texCoord + BlurDir * t, vec2(0.002), vec2(0.998));
        sum += texture(InSampler, uv) * w;
        total += w;
    }

    fragColor = sum / total;
}
