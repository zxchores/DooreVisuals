#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

in vec4 vertexColor;
in vec3 viewPos;
in vec3 localPos;

out vec4 fragColor;

void main() {
    if (vertexColor.a <= 0.001) {
        discard;
    }

    // Fade the aura toward the ends of the forearm and soften it toward the shell's outer edges.
    float radial = length(vec2(localPos.x * 4.2, localPos.z * 4.2));
    float rim = 1.0 - smoothstep(0.2, 1.05, radial);
    float along = smoothstep(-0.24, -0.08, localPos.y) * (1.0 - smoothstep(0.32, 0.48, localPos.y));
    float distanceFade = clamp(1.15 - length(viewPos) * 0.01, 0.5, 1.0);
    float alpha = vertexColor.a * mix(0.38, 1.0, rim) * along * distanceFade;
    if (alpha <= 0.002) {
        discard;
    }

    vec3 rgb = doore_toLinear(vertexColor.rgb) * (1.08 + alpha * 0.48);
    rgb = doore_toSrgb(doore_tonemap(rgb));
    rgb += doore_dither(gl_FragCoord.xy);
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
