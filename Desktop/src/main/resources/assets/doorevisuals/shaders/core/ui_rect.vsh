#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV2;

out vec4 vertexColor;
out vec2 fieldPos;
out vec2 shapeParams;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;

    // UV0 already holds the corner-relative coordinate the distance field needs, so the fragment
    // stage never has to know where the rectangle sits on screen.
    fieldPos = UV0;

    // Radius always, then either an inset border width or a negative blur radius. Both arrive as
    // sixteenths of a pixel because the slot is a short and a whole pixel would be too coarse for
    // small corner radii.
    shapeParams = vec2(UV2) * 0.0625;
}
