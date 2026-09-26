#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

in vec4 vertexColor;
in vec3 localPos;
in vec2 sunParams;

out vec4 fragColor;

// This vertex format has no spare attribute for the sun, so the whole shell carries the same pair of
// texture coordinates: x is the azimuth in turns, y is the sine of the elevation remapped to 0..1
// with 2.0 added when the client draws its own sun and moon instead of the vanilla ones.
vec3 sunDirection(vec2 params, out float sunUp, out float ownDiscs) {
    ownDiscs = step(1.5, params.y);
    sunUp = (params.y - ownDiscs * 2.0) * 2.0 - 1.0;
    float az = params.x * 6.2831853;
    float radial = sqrt(max(0.0, 1.0 - sunUp * sunUp));
    return normalize(vec3(cos(az) * radial, sunUp, sin(az) * radial));
}

void main() {
    if (vertexColor.a <= 0.001) {
        discard;
    }

    float sunUp;
    float ownDiscs;
    vec3 sun = sunDirection(sunParams, sunUp, ownDiscs);
    vec3 dir = normalize(localPos);

    // Air mass grows toward the horizon, so scattered light piles up in a band there.
    float horizon = pow(1.0 - abs(dir.y), 3.2);
    float mu = dot(dir, sun);

    // Henyey-Greenstein, strongly forward scattering: the halo that surrounds a low sun.
    float g = 0.76;
    float mie = (1.0 - g * g) / pow(1.0 + g * g - 2.0 * g * mu, 1.5) * 0.0795775;

    // Sunlight reddens as it crosses more air, so tie its colour to how high the sun sits.
    vec3 sunTint = mix(vec3(1.0, 0.40, 0.14), vec3(1.0, 0.95, 0.88), clamp(sunUp * 1.7 + 0.28, 0.0, 1.0));
    float daylight = clamp(sunUp * 3.2 + 0.32, 0.0, 1.0);
    vec3 sunLin = doore_toLinear(sunTint);

    vec3 scatter = sunLin * mie * (0.34 + 0.66 * horizon) * daylight * 1.4;
    scatter += sunLin * horizon * daylight * 0.24;

    float alpha = vertexColor.a;
    if (ownDiscs > 0.5) {
        float night = 1.0 - daylight;
        float sunDisc = 1.0 - smoothstep(0.021, 0.031, acos(clamp(mu, -1.0, 1.0)));
        float moonDisc = 1.0 - smoothstep(0.025, 0.036, acos(clamp(-mu, -1.0, 1.0)));
        scatter += sunLin * sunDisc * 7.0 * daylight;
        scatter += doore_toLinear(vec3(0.86, 0.91, 1.0)) * moonDisc * 2.4 * night;
        alpha = max(alpha, max(sunDisc * daylight, moonDisc * night));
    }

    // The authored gradient is left exactly as it is when nothing is scattering; everything added on
    // top is rolled off and screened in, so a bright sun brightens the sky without clipping to white.
    vec3 base = doore_toLinear(vertexColor.rgb);
    vec3 rgb = doore_toSrgb(base + doore_tonemap(scatter) * (1.0 - base));
    rgb += doore_dither(gl_FragCoord.xy);
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
