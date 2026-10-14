#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec4 vertexColor;
out vec2 texCoord;
out vec3 localPos;
out vec3 viewPos;
out vec3 normal;

// The attribute list has to match POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL exactly: this pipeline
// draws the same model part vanilla draws, so it receives the full player-model vertex format.
void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    vertexColor = Color;
    texCoord = UV0;
    localPos = Position;
    viewPos = view.xyz;
    normal = Normal;
}
