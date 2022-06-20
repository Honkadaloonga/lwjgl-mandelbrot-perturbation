#version 460
precision highp float;

in vec3 a_pos;

out vec2 pos;

void main(){
    pos = a_pos.xy;
    gl_Position = vec4(a_pos, 1.0);
}