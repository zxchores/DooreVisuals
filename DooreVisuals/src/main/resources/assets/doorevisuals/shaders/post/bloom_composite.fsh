#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;
uniform sampler2D BloomSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform CompositeConfig {
    float BloomStrength;
    float Contrast;
    float Saturation;
    float Vignette;
    float Grain;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 scene = texture(InSampler, texCoord).rgb;
    vec3 bloom = texture(BloomSampler, texCoord).rgb;

    vec3 color = scene + bloom * BloomStrength;

    // Contrast around mid grey, then saturation, so the bloom reads as light rather than as fog.
    color = (color - 0.5) * Contrast + 0.5;
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(luma), color, Saturation);

    // Vignette and a slow grain, both deliberately mild: they should be felt, not noticed.
    vec2 d = texCoord - 0.5;
    color *= 1.0 - Vignette * smoothstep(0.18, 1.0, dot(d, d) * 4.0);

    float noise = fract(sin(dot(gl_FragCoord.xy + vec2(GameTime * 0.37, GameTime * 0.21), vec2(12.9898, 78.233))) * 43758.5453);
    color += (noise - 0.5) * Grain;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
