#version 330



#moj_import <minecraft:dynamictransforms.glsl>

#moj_import <minecraft:globals.glsl>

#moj_import <doorevisuals:fx.glsl>



in vec4 vertexColor;

in vec3 viewPos;

in vec3 localPos;



out vec4 fragColor;



void main() {

    vec4 color = vertexColor;

    if (color.a <= 0.001) {

        discard;

    }

    float dist = length(viewPos);

    vec3 viewDir = normalize(-viewPos);

    float radial = length(localPos.xz);

    float height = localPos.y;

    float shell = smoothstep(0.05, 0.75, radial * 0.5 + abs(height) * 0.06);

    vec3 nApprox = normalize(vec3(localPos.x, 0.55, localPos.z) + vec3(0.001));

    float fresnel = pow(clamp(1.0 - abs(dot(viewDir, nApprox)), 0.0, 1.0), 1.6);

    float haze = 0.7 + 0.3 * doore_fbm(localPos.xz * 1.4 + vec2(height * 0.3, GameTime * 6.0));

    float soft = doore_softDistance(viewPos);

    vec3 cool = color.rgb * vec3(0.8, 0.95, 1.2);

    vec3 rgb = mix(cool * 0.85, color.rgb * 1.25, fresnel) * haze;

    // Keep body visible; fade only at long range (correct smoothstep order).

    float distFade = 1.0 - smoothstep(24.0, 72.0, dist);

    float alpha = color.a * soft * (0.45 + shell * 0.25 + fresnel * 0.35) * (0.55 + distFade * 0.45);

    fragColor = vec4(rgb, clamp(alpha, 0.05, 0.9)) * ColorModulator;

}

