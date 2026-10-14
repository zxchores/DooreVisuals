#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec4 vertexColor;
out vec3 localPos;
out vec2 sunParams;
flat out ivec2 celestialColors;
flat out ivec2 skyParams;
out vec3 skyExtra;

void main() {
    localPos = Position;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    sunParams = UV0;
    celestialColors = UV2;
    // Two more data channels the sky needs: the lunar phase and the cloud coverage (UV1), plus the
    // sun visibility used to switch the shafts off behind terrain (Normal).
    skyParams = UV1;
    skyExtra = Normal;
}
