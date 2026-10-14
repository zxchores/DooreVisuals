#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <doorevisuals:fx.glsl>

in vec4 vertexColor;
in vec2 fieldPos;
in vec2 shapeParams;

out vec4 fragColor;

void main() {
    float radius = max(shapeParams.x, 0.0);
    float border = max(shapeParams.y, 0.0);
    float blur = max(-shapeParams.y, 0.0);

    // Signed distance to a rounded rectangle. fieldPos is |p| - (halfSize - radius), which the CPU
    // side can hand over directly because every rectangle is split into its four quadrants and the
    // absolute value is therefore linear across each one.
    vec2 outside = max(fieldPos, vec2(0.0));
    float dist = length(outside) + min(max(fieldPos.x, fieldPos.y), 0.0) - radius;

    // One screen pixel measured in field units, so the edge stays a pixel wide at any GUI scale.
    float pixel = max(fwidth(dist), 0.0001);

    float coverage;
    if (blur > 0.0) {
        coverage = 1.0 - smoothstep(-blur, blur, dist);
    } else if (border > 0.0) {
        // Keep the stroke inside the rectangle, the way a one pixel outline is expected to sit.
        coverage = clamp(0.5 - max(dist, -(dist + border)) / pixel, 0.0, 1.0);
    } else {
        coverage = clamp(0.5 - dist / pixel, 0.0, 1.0);
    }

    float alpha = vertexColor.a * coverage;
    if (alpha <= 0.0015) {
        discard;
    }

    // Panel fills are wide, low contrast gradients, which is exactly where 8-bit steps show up.
    vec3 rgb = vertexColor.rgb + doore_dither(gl_FragCoord.xy);
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
