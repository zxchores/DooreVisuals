#version 330



#moj_import <minecraft:dynamictransforms.glsl>

#moj_import <minecraft:globals.glsl>



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

    float fade = clamp(1.2 - dist * 0.012, 0.45, 1.0);

    fragColor = vec4(color.rgb, color.a * fade) * ColorModulator;

}


