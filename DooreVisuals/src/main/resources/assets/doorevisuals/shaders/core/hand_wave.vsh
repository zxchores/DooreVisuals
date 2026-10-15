#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec4 vertexColor;
out vec2 texCoord;
out vec3 viewPos;
out vec3 normal;
// Settings of the effect, carried by the vertices of the model part (no extra texture or uniform is needed):
//   Color  = the primary colour of the waves, its alpha is the opacity (the item variant takes both from UV2)
//   UV1    = (speed, wave size) in thousandths
//   UV2    = the secondary colour: x = r | g << 8, y = b
flat out vec2 ctl;
flat out vec3 secondary;

// The attribute list has to match POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL exactly: this pipeline draws the same
// model part vanilla draws. The position is computed in the same order as the vanilla entity shader, so the depth of this
// pass matches the depth of the arm that was drawn first and the waves do not fight with it.
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = UV0;
    viewPos = (ModelViewMat * vec4(Position, 1.0)).xyz;
    normal = Normal;
    ctl = vec2(UV1) / 1000.0;
#ifdef DOORE_WAVE_ITEM
    // The item in the hand: r, g, b and the opacity all travel in the light value (r | g << 8 | b << 16 | a << 24).
    vertexColor = vec4(float(UV2.x & 255), float((UV2.x >> 8) & 255), float(UV2.y & 255), float((UV2.y >> 8) & 255)) / 255.0;
    secondary = mix(vertexColor.rgb, vec3(1.0), 0.4);
#else
    vertexColor = Color;
    secondary = vec3(float(UV2.x & 255), float((UV2.x >> 8) & 255), float(UV2.y & 255)) / 255.0;
#endif
}
