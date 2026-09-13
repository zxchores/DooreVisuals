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

    float fresnel = pow(clamp(1.0 - abs(dot(viewDir, normalize(vec3(localPos.x, 0.45, localPos.z)))), 0.0, 1.0), 2.0);

    float heightBand = 0.65 + 0.35 * smoothstep(0.0, 1.0, fract(localPos.y * 0.35));

    float grid = max(

        abs(fract(localPos.x * 2.2) - 0.5),

        abs(fract(localPos.z * 2.2) - 0.5)

    );

    float panel = 1.0 - smoothstep(0.42, 0.5, grid * 2.0);

    vec3 rgb = color.rgb * (0.55 + fresnel * 0.7 + panel * 0.2) * heightBand;

    float alpha = color.a * (0.2 + fresnel * 0.45 + panel * 0.15) * doore_softDistance(viewPos);

    fragColor = vec4(rgb, clamp(alpha, 0.0, 0.88)) * ColorModulator;

}

