#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

in vec4 vertexColor;
in vec2 noisePos;
in vec3 sunDirection;

out vec4 fragColor;

// The plane is a disc whose vertex alpha already fades to nothing at the rim, and whose texture
// coordinates are world position scaled into the noise domain plus a scroll the client advances.
// The sun direction rides in the normal channel. Lighting is faked from the noise itself: the field is sampled a
// little way toward the sun, and where that is denser the point lies in the shadow of the cloud in front of it.
void main() {
    float cover = vertexColor.a;
    if (cover <= 0.002) {
        discard;
    }

    float shape = doore_fbmWarp(noisePos, 1.15);
    float edge = smoothstep(0.44, 0.80, shape);
    if (edge <= 0.002) {
        discard;
    }

    float detail = doore_fbm(noisePos * 3.3);
    float fine = doore_fbm(noisePos * 8.1 + 5.3);
    float alpha = edge * cover * (0.60 + 0.34 * detail + 0.06 * fine);
    if (alpha <= 0.003) {
        discard;
    }

    vec3 sunDir = normalize(sunDirection + vec3(0.0, 0.0001, 0.0));
    float sunUp = sunDir.y;
    vec2 toSun = normalize(sunDir.xz + vec2(0.0001));

    // Low sun, long shadows: the sample reaches further toward it near the horizon.
    float reach = mix(0.26, 0.08, clamp(sunUp * 2.0, 0.0, 1.0));
    float ahead = doore_fbmWarp(noisePos + toSun * reach, 1.15);
    float sunlit = clamp(0.58 + (shape - ahead) * 1.9, 0.0, 1.0);

    // Thin edges that face the sun glow: the silver lining.
    float rim = smoothstep(0.44, 0.52, shape) * (1.0 - smoothstep(0.52, 0.70, shape));
    float lining = rim * sunlit;

    // Warm light near sunrise and sunset, white at noon; at night there is no sun to tint with.
    float sunWeight = smoothstep(-0.08, 0.04, sunUp);
    float lowSun = sunWeight * (1.0 - smoothstep(0.08, 0.5, sunUp));
    vec3 warm = mix(vec3(1.0), vec3(1.0, 0.60, 0.38), lowSun);

    vec3 base = doore_toLinear(vertexColor.rgb);
    float shade = mix(0.70, 1.12, sunlit) * (0.80 + 0.30 * smoothstep(0.46, 0.9, shape));
    vec3 lit = base * shade * warm + warm * lining * 0.55 * sunWeight;
    vec3 rgb = doore_toSrgb(lit);
    rgb += doore_dither(gl_FragCoord.xy);
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
