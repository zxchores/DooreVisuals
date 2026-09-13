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

    vec3 viewDir = normalize(-viewPos);

    float fresnel = pow(clamp(1.0 - abs(dot(viewDir, normalize(vec3(localPos.x, 0.5, localPos.z)))), 0.0, 1.0), 2.4);

    float depth = doore_softDistance(viewPos);

    float grain = doore_noise(localPos.xz * 3.0 + localPos.y * 1.2);

    vec3 rgb = color.rgb;

    rgb.r *= 1.0 + fresnel * 0.35;

    rgb.b *= 1.0 + fresnel * 0.55;

    rgb.g *= 0.95 + grain * 0.08;

    float alpha = color.a * (0.18 + fresnel * 0.55 + grain * 0.08) * depth;

    fragColor = vec4(rgb, clamp(alpha, 0.0, 0.85)) * ColorModulator;

}

