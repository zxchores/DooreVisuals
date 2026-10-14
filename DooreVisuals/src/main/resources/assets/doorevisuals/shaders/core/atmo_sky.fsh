#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

in vec4 vertexColor;
in vec3 localPos;
in vec2 sunParams;
flat in ivec2 celestialColors;
flat in ivec2 skyParams;
in vec3 skyExtra;

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

vec3 unpack565(int packedColor) {
    return vec3(
        float((packedColor >> 11) & 31) / 31.0,
        float((packedColor >> 5) & 63) / 63.0,
        float(packedColor & 31) / 31.0
    );
}

float h11(float x) {
    return fract(sin(x * 127.1) * 43758.5453123);
}

float h31(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);
}

/**
 * Stars on a 3D grid around the camera, so the density is the same over the whole sphere (a latitude/longitude grid
 * crowds them at the poles). A cell hosts at most one star; its point sits inside the cell and the star is drawn where
 * the sky sphere passes close to that point. Every star has a rank: the low ranks are the first to light up as the sun
 * sinks (the argument "deep" grows from 0 at dusk to 1 at the dead of night), so the evening starts with a few small dots
 * and fills up slowly.
 */
float doore_stars(vec3 dir, float time, float deep) {
    const float N = 34.0;
    vec3 p = dir * N;
    vec3 cell = floor(p);
    if (h31(cell) < 0.62) {
        return 0.0;
    }

    vec3 pos = vec3(h31(cell + 4.1), h31(cell + 9.7), h31(cell + 15.3)) * 0.6 + 0.2;
    float d = length(fract(p) - pos);
    float rank = sqrt(h31(cell + 21.3));
    float rad = mix(0.085, 0.045, rank);
    float core = 1.0 - smoothstep(rad * 0.35, rad, d);
    float appear = smoothstep(rank * 0.9, rank * 0.9 + 0.08, deep);
    float twinkle = 0.72 + 0.28 * sin(time * (1.3 + 2.1 * rank) + rank * 47.0);
    float magnitude = mix(1.0, 0.32, rank);
    return core * appear * twinkle * magnitude;
}

/**
 * Shooting stars. Time is cut into slots of one period; each slot may hold one meteor, chosen by hash against the rate.
 * A meteor flies along a great circle from a random point in the upper sky: a thin bright streak with a fading tail and a
 * glowing head. Two streams with different periods overlap so they do not fall in a rhythm.
 */
float doore_meteor(vec3 dir, float t, float period, float salt, float rate) {
    float slot = floor(t / period);
    float u = (t - slot * period) / 1.15;
    float s = slot + salt;
    if (u >= 1.0 || h11(s) > rate) {
        return 0.0;
    }

    float az = h11(s + 3.7) * 6.2831853;
    float el = mix(0.30, 0.95, h11(s + 8.1));
    float rad = sqrt(1.0 - el * el);
    vec3 a = vec3(cos(az) * rad, el, sin(az) * rad);
    vec3 rnd = normalize(vec3(h11(s + 11.3) - 0.5, h11(s + 14.9) - 0.5, h11(s + 19.7) - 0.5) + 0.0001);
    vec3 tangent = normalize(rnd - a * dot(rnd, a));
    tangent = normalize(tangent - vec3(0.0, 0.7, 0.0));
    tangent = normalize(tangent - a * dot(tangent, a));
    vec3 n = cross(a, tangent);

    float x = dot(dir, a);
    float y = dot(dir, tangent);
    float z = dot(dir, n);
    if (x <= 0.0) {
        return 0.0;
    }

    float arc = 0.55;
    float trail = 0.22;
    float head = u * arc;
    float along = head - atan(y, x);
    if (along < -0.004 || along > trail) {
        return 0.0;
    }

    float k = clamp(along / trail, 0.0, 1.0);
    float width = 0.0011 + 0.0010 * k;
    float streak = exp(-(z * z) / (width * width));
    float tail = pow(1.0 - k, 1.8);
    float headGlow = exp(-(along * along) / 0.00005);
    float env = smoothstep(0.0, 0.12, u) * (1.0 - smoothstep(0.65, 1.0, u));
    return streak * (tail + headGlow * 1.6) * env;
}

/**
 * The cloud field lives on its own shell, but the same noise projected onto a plane lines up with it
 * closely enough that the celestial discs are dimmed exactly where a cloud drifts across them.
 */
float doore_cloudMask(vec3 dir, float coverage, float time) {
    if (coverage <= 0.002 || dir.y <= 0.02) {
        return 0.0;
    }

    vec2 p = dir.xz / dir.y;
    float n = doore_fbm(p * 0.32 + vec2(time * 0.0045, time * 0.0021));
    return smoothstep(1.0 - coverage, 1.0 - coverage + 0.24, n);
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
    vec3 authoredSun = unpack565(celestialColors.x);
    vec3 authoredMoon = unpack565(celestialColors.y);
    vec3 sunTint = mix(vec3(1.0, 0.40, 0.14), authoredSun, clamp(sunUp * 1.7 + 0.28, 0.0, 1.0));
    float daylight = clamp(sunUp * 3.2 + 0.32, 0.0, 1.0);
    float night = 1.0 - daylight;
    vec3 sunLin = doore_toLinear(sunTint);

    // Terrain between the camera and the sun switches the shafts off; the rest of the sky stays.
    float sunVisibility = clamp(skyExtra.x * 0.5 + 0.5, 0.0, 1.0);

    // Narrow angular bands form sun shafts in the sky pass. Terrain and foliage drawn later keep
    // their depth, while the translucent cloud field softens the shafts instead of hiding them.
    float angleToSun = acos(clamp(mu, -1.0, 1.0));
    float azimuth = atan(dir.z, dir.x) - atan(sun.z, sun.x);
    float wrappedAzimuth = atan(sin(azimuth), cos(azimuth));
    float spokes = pow(max(0.0, 0.5 + 0.5 * cos(wrappedAzimuth * 26.0 + sin(angleToSun * 17.0) * 1.7)), 18.0);
    float rayFalloff = (1.0 - smoothstep(0.025, 0.48, angleToSun)) * smoothstep(0.012, 0.06, angleToSun);
    float rayNoise = 0.76 + 0.24 * sin(wrappedAzimuth * 71.0 + GameTime * 1800.0);
    float shafts = spokes * rayFalloff * rayNoise * daylight * sunVisibility;

    vec3 scatter = sunLin * mie * (0.34 + 0.66 * horizon) * daylight * 1.4;
    scatter += sunLin * horizon * daylight * 0.24;
    scatter += sunLin * shafts * (0.10 + 0.22 * horizon);

    float coverage = float(skyParams.y) / 65535.0;
    float clouds = doore_cloudMask(dir, coverage, GameTime);

    float alpha = vertexColor.a;

    if (ownDiscs > 0.5) {
        float moonPhase = float(skyParams.x) / 65535.0;

        // Sun: a bright disc, a touch darker toward the rim, with a tight corona and a wide soft one around it.
        float sunDisc = 1.0 - smoothstep(0.021, 0.031, angleToSun);
        float sunR = clamp(angleToSun / 0.031, 0.0, 1.0);
        float sunLimb = 0.86 + 0.14 * sqrt(max(0.0, 1.0 - sunR * sunR));
        float corona = exp(-angleToSun * 22.0) * 0.55 + exp(-angleToSun * 6.5) * 0.16;

        // Moon: shaded as a real sphere. The normal of each point on the disc is lit from a direction that turns with
        // the phase, so the terminator is a soft curved streak and the crescent keeps its shape. Dark maria and fine
        // crater noise sit on top, the rim is darkened, and a faint halo surrounds the lit part.
        vec3 moonDir = -sun;
        vec3 upRef = abs(moonDir.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
        vec3 rightAxis = normalize(cross(upRef, moonDir));
        vec3 upAxis = cross(moonDir, rightAxis);
        vec3 rel = dir - moonDir * dot(dir, moonDir);
        vec2 q = vec2(dot(rel, rightAxis), dot(rel, upAxis)) / 0.031;
        float r2 = dot(q, q);
        float moonMask = (1.0 - smoothstep(0.88, 1.04, sqrt(r2))) * step(0.0, dot(dir, moonDir));
        float zq = sqrt(max(0.0, 1.0 - r2));
        float phaseAngle = moonPhase * 6.2831853;
        vec3 lightDir = normalize(vec3(-sin(phaseAngle), 0.12, cos(phaseAngle)));
        float lambert = smoothstep(-0.03, 0.22, dot(vec3(q, zq), lightDir));
        float maria = smoothstep(0.50, 0.62, doore_fbm(q * 1.7 + 3.1));
        float craters = doore_fbm(q * 7.0 + 11.0);
        float albedo = 0.88 - 0.38 * maria + 0.10 * (craters - 0.5);
        float limb = pow(max(zq, 0.0), 0.28);
        // A sliver of earthshine keeps a new moon from turning into a black hole in the sky.
        float moonLight = moonMask * (0.07 * limb + lambert * albedo * limb) * (1.0 - 0.9 * clouds);
        float moonAngle = acos(clamp(-mu, -1.0, 1.0));
        float litFraction = 0.5 + 0.5 * cos(phaseAngle);
        float moonHalo = exp(-max(moonAngle - 0.03, 0.0) * 38.0) * (0.10 + 0.22 * litFraction) * (1.0 - 0.9 * clouds);
        float sunLight = sunDisc * sunLimb * (1.0 - 0.9 * clouds);

        scatter += sunLin * sunLight * 7.0 * daylight;
        scatter += sunLin * corona * daylight * sunVisibility * (1.0 - 0.7 * clouds);
        scatter += doore_toLinear(authoredMoon) * (moonLight * 2.6 + moonHalo) * night;
        alpha = max(alpha, max(sunLight * daylight, (moonLight + moonHalo * 0.5) * night));
    }

    // How far the sun is below the horizon: 0 at dusk, 1 in the dead of night. It paces the stars (a few at first,
    // more as the night goes on) and holds the shooting stars back until it is properly dark.
    float deep = smoothstep(-0.02, -0.8, sunUp);
    float starWash = night * (1.0 - 0.88 * horizon) * (1.0 - 0.85 * clouds);

    // The shader's own stars replace the vanilla ones only together with the own sun and moon; otherwise the vanilla
    // star field is still drawn and a second one on top of it would double them up.
    if (ownDiscs > 0.5) {
        scatter += vec3(0.80, 0.86, 1.0) * doore_stars(dir, GameTime * 55.0, deep) * starWash * 2.4;
    }

    float meteorRate = clamp(skyExtra.y * 0.5 + 0.5, 0.0, 1.0);
    if (meteorRate > 0.01 && dir.y > 0.04 && deep > 0.85) {
        float tm = GameTime * 1200.0;
        float m = doore_meteor(dir, tm, 5.3, 0.0, meteorRate) + doore_meteor(dir, tm, 8.7, 41.0, meteorRate);
        scatter += vec3(0.86, 0.93, 1.0) * m * smoothstep(0.85, 1.0, deep) * (1.0 - 0.9 * clouds) * (0.35 + 0.65 * (1.0 - horizon)) * 3.2;
    }

    // The authored gradient is left exactly as it is when nothing is scattering; everything added on
    // top is rolled off and screened in, so a bright sun brightens the sky without clipping to white.
    vec3 base = doore_toLinear(vertexColor.rgb);
    vec3 rgb = doore_toSrgb(base + doore_tonemap(scatter) * (1.0 - base));
    rgb += doore_dither(gl_FragCoord.xy);
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
