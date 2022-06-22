#version 460
precision highp float;

in vec2 pos;
out vec4 fragColor;

const int maxIter = maxIter;

uniform vec2 res;
uniform float mag;
uniform samplerBuffer sampler;

// I love fourier transforms
vec3 getCol(float v) {
    return vec3(
        0.537 + cos(v+0.834)*0.474 + cos(v*2.0-2.655)*0.104 + cos(v*3.0-0.187)*0.023,
        0.491 + cos(v+1.702)*0.489 + cos(v*2.0+2.034)*0.043,
        0.645 + cos(v+1.804)*0.361 + cos(v*2.0+1.535)*0.037
    );
}

void main() {
    vec2 dc = pos/mag*vec2(res.x/res.y, 1.0);
    vec2 dz = vec2(0, 0);
    vec3 col = vec3(0, 0, 0);

    vec2 z, ndz, t;
    float m, br;
    for (int i = 0; i < maxIter; i++) {
        z = texelFetch(sampler, i).xy;
        ndz.x = 2.0*(z.x*dz.x - z.y*dz.y) + dz.x*dz.x - dz.y*dz.y + dc.x;
        ndz.y = 2.0*(z.x*dz.y + z.y*dz.x + dz.x*dz.y) + dc.y;
        t = z + ndz;
        m = dot(t, t);
        if (m > 1024.0) {
            br = 2.0 + float(i) - log(log(m) * 0.5) * 1.442695;
            col = getCol(sqrt(br));
            break;
        }
        dz = ndz;
    }

    fragColor = vec4(col, 1.0);
}