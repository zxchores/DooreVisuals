float doore_hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float doore_noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = doore_hash(i);
    float b = doore_hash(i + vec2(1.0, 0.0));
    float c = doore_hash(i + vec2(0.0, 1.0));
    float d = doore_hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float doore_fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 m = mat2(1.6, 1.2, -1.2, 1.6);
    for (int i = 0; i < 4; i++) {
        v += a * doore_noise(p);
        p = m * p;
        a *= 0.5;
    }
    return v;
}

// Six octaves for the sky, where the extra detail is worth the cost.
float doore_fbm6(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 m = mat2(1.6, 1.2, -1.2, 1.6);
    for (int i = 0; i < 6; i++) {
        v += a * doore_noise(p);
        p = m * p;
        a *= 0.5;
    }
    return v;
}

// Feeding the field back into its own coordinates breaks up the grid the value noise leaves behind.
float doore_fbmWarp(vec2 p, float amount) {
    vec2 q = vec2(doore_fbm6(p), doore_fbm6(p + vec2(5.2, 1.3)));
    return doore_fbm6(p + amount * q);
}

float doore_pulse(float time, float speed, float amount) {
    return 1.0 - amount + amount * (0.5 + 0.5 * sin(time * speed));
}

// The blend and lighting maths below only behave sensibly on linear values, while vertex colours and
// the framebuffer are sRGB, so anything doing real light transport converts on the way in and out.
vec3 doore_toLinear(vec3 c) {
    return pow(max(c, vec3(0.0)), vec3(2.2));
}

vec3 doore_toSrgb(vec3 c) {
    return pow(max(c, vec3(0.0)), vec3(1.0 / 2.2));
}

// Narkowicz's fit of the ACES curve: keeps bright scattering from clipping to flat white.
vec3 doore_tonemap(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

// One step of ordered noise, enough to hide banding in the wide smooth gradients of a sky.
float doore_dither(vec2 frag) {
    float n = fract(dot(frag, vec2(0.06711056, 0.00583715)));
    return (fract(52.9829189 * n) - 0.5) / 255.0;
}

float doore_softDistance(vec3 viewPos) {
    float dist = length(viewPos);
    return clamp(1.2 - dist * 0.016, 0.35, 1.0);
}
