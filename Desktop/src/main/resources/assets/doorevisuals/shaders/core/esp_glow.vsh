#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;
out vec3 viewPos;
out vec3 localPos;

void main() {
    localPos = Position;
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    viewPos = view.xyz;
    gl_Position = ProjMat * view;
    vertexColor = Color;
}
