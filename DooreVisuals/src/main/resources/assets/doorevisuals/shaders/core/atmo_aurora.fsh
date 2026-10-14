#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <doorevisuals:fx.glsl>

in vec4 vertexColor;
in vec2 bandPos;
out vec4 fragColor;

// bandPos.x runs along the curtain (it already carries the scroll the client advances), bandPos.y is the height
// across it, 0 at the lower edge and 1 at the top. A real aurora is made of vertical rays: a sharp bright lower edge
// that fades out exponentially upward, rays of different heights side by side, slow pulses travelling along the arc,
// and a colour that climbs from the base colour to a red fringe at the tips of the tallest rays.
void main() {
    float h = clamp(bandPos.y, 0.0, 1.0);
    float x = bandPos.x;
    float tt = GameTime * 1200.0;

    // Rays: noise across the curtain, almost constant along its height, wobbled by a slow warp.
    float warp = doore_fbm(vec2(x * 1.3, tt * 0.02)) * 0.35;
    float rays = doore_noise(vec2(x * 26.0 + warp * 8.0, tt * 0.25));
    float fineRays = doore_noise(vec2(x * 61.0 - warp * 5.0, tt * 0.4 + 7.0));
    float rayField = smoothstep(0.18, 0.95, 0.55 * rays + 0.30 * fineRays + 0.15);

    // Each ray reaches its own height.
    float reach = 0.30 + 0.65 * doore_fbm(vec2(x * 3.1 + 11.0, tt * 0.03));
    float topFade = 1.0 - smoothstep(reach * 0.55, reach, h);

    float lowerEdge = smoothstep(0.0, 0.035, h);
    float decay = exp(-h * 3.4);
    float hotEdge = exp(-h * 22.0);

    // Pulses travel along the arc; patches come and go over minutes.
    float pulse = 0.72 + 0.28 * sin(x * 3.0 - tt * 0.35 + doore_noise(vec2(x * 2.0, 3.0)) * 6.0);
    float clump = smoothstep(0.20, 0.70, doore_fbmWarp(vec2(x * 1.4 + 3.0, tt * 0.012), 0.5));

    float light = lowerEdge * (decay * (0.35 + 0.65 * rayField) * topFade + hotEdge * 0.55 * rayField) * pulse * clump;
    if (light <= 0.004 || vertexColor.a <= 0.002) discard;

    vec3 base = doore_toLinear(vertexColor.rgb);
    vec3 fringe = vec3(0.95, 0.15, 0.35);
    vec3 color = mix(base, fringe, smoothstep(0.55, 0.95, h) * 0.30 * rayField);
    color += vec3(0.55, 1.0, 0.8) * hotEdge * 0.35;

    vec3 luminous = color * (0.9 + light * 1.8);
    vec3 rgb = doore_toSrgb(doore_tonemap(luminous));
    rgb += doore_dither(gl_FragCoord.xy);

    float alpha = clamp(vertexColor.a * light * 1.25, 0.0, 0.85);
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
