#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

in vec4 vertexColor;
in vec2 noisePos;

out vec4 fragColor;

// The plane is a disc whose vertex alpha already fades to nothing at the rim, and whose texture
// coordinates are world position scaled into the noise domain plus a scroll the client advances.
void main() {
    float cover = vertexColor.a;
    if (cover <= 0.002) {
        discard;
    }

    float shape = doore_fbmWarp(noisePos, 1.15);
    float edge = smoothstep(0.46, 0.80, shape);
    if (edge <= 0.002) {
        discard;
    }

    float detail = doore_fbm(noisePos * 3.3);
    float alpha = edge * cover * (0.62 + 0.38 * detail);
    if (alpha <= 0.003) {
        discard;
    }

    // Thicker parts of the field read as lit tops, thin parts as translucent fringes.
    vec3 base = doore_toLinear(vertexColor.rgb);
    vec3 rgb = doore_toSrgb(base * (0.78 + 0.46 * edge));
    rgb += doore_dither(gl_FragCoord.xy);
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
