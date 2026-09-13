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

    float fresnel = pow(clamp(1.0 - abs(dot(viewDir, normalize(vec3(localPos.x, 0.4, localPos.z)))), 0.0, 1.0), 1.8);

    vec2 flow = localPos.xz * 2.4 + vec2(GameTime * 18.0, localPos.y * 0.8);

    float n1 = doore_fbm(flow);

    float n2 = doore_fbm(flow * 1.6 + 11.0);

    float body = smoothstep(0.2, 0.85, n1 * 0.6 + n2 * 0.4);

    vec3 cool = color.rgb * vec3(0.6, 0.8, 1.2);

    vec3 warm = color.rgb * vec3(1.25, 1.0, 0.75);

    vec3 rgb = mix(cool, warm, body) * (0.75 + fresnel * 0.7);

    rgb += warm * body * fresnel * 0.35;

    float alpha = color.a * (0.28 + body * 0.35 + fresnel * 0.4) * doore_softDistance(viewPos);

    fragColor = vec4(rgb, clamp(alpha, 0.0, 0.9)) * ColorModulator;

}

