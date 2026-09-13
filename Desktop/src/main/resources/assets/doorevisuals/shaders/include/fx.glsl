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

float doore_pulse(float time, float speed, float amount) {
    return 1.0 - amount + amount * (0.5 + 0.5 * sin(time * speed));
}

float doore_softDistance(vec3 viewPos) {
    float dist = length(viewPos);
    return clamp(1.2 - dist * 0.016, 0.35, 1.0);
}
