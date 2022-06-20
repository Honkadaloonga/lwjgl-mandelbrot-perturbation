#version 460
precision highp float;

in vec2 pos;
out vec4 fragColor;

uniform int maxIter = 256;

uniform vec2 res;
uniform vec2 center;
uniform float mag;
uniform float gnio;


//vec2 mul(vec2 a, vec2 b) {
//    return vec2(a.x*b.x-a.y*b.y, a.x*b.y+a.y*b.x);
//}

vec2 sqr(vec2 a) {
    return vec2(a.x*a.x - a.y*a.y, 2.0*a.x*a.y);
}

vec3 getCol(float v) {
    float red = 0.537 + cos(v+0.834)*0.474 + cos(v*2.0-2.655)*0.104 + cos(v*3.0-0.187)*0.023;
    float grn = 0.491 + cos(v+1.702)*0.489 + cos(v*2.0+2.034)*0.043;
    float blu = 0.645 + cos(v+1.804)*0.361 + cos(v*2.0+1.535)*0.037;
    return vec3(red, grn, blu);
}

void main() {
    vec2 c = pos/mag*vec2(res.x/res.y, 1.0) + center;
    vec2 z = vec2(0, 0);

    vec3 col = vec3(0, 0, 0);
    float m = 0.0;
    for (int i = 0; i < maxIter; i++) {
        z = sqr(z) + c;
        m = float(dot(z, z));
        if (m > 1024.0) {
            float br = float(i) + 2.0 - log(log(m) * 0.5) * 1.442695;
            col = getCol(0.4 * sqrt(br));
            break;
        }
    }

    fragColor = vec4(col, 1.0);
}