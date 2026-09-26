#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec4 vertexColor;
out vec3 localPos;
out vec2 sunParams;

void main() {
    localPos = Position;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    sunParams = UV0;
}
