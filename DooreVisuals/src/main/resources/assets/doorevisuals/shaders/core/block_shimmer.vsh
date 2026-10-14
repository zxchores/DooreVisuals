#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;
in vec3 Normal;
out vec4 vertexColor;
out vec3 viewPos;
out float phaseCoord;
out float shaderSpeed;

void main() {
    phaseCoord = Normal.z;
    shaderSpeed = clamp(abs(Normal.x), 0.15, 2.5);
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    viewPos = view.xyz;
    gl_Position = ProjMat * view;
    vertexColor = Color;
}
