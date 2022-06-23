package me.honka;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class FractalRenderer {
    private static final int width = 3840;
    private static final int height = 2160;
    private static final int aaLevel = 1;

    private static final BigDecimal centerX = new BigDecimal("-1.769233641266822788211");
    private static final BigDecimal centerY = new BigDecimal("0.003412911653518676758");
    private static final float mag = 1e8f;

    private static final int maxIter = 16384;
    final float[] orbitValues = new float[maxIter*2];

    private long window;
    private int program;
    private int VAO;

    //TODO add antialiasing to the fragment shader

    public static void main(String[] args) {
        System.out.println("Starting");

        try {
            new FractalRenderer().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FractalRenderer() {
        initOrbitValues();
        initGLFWWindow();
    }

    private void run() throws Exception {
        initProgram();
        initVertices();
        initUniforms();

        while (!GLFW.glfwWindowShouldClose(window)) {
            render();
            save();

            GLFW.glfwSetWindowShouldClose(window, true);
        }

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    private void initOrbitValues() {
        MathContext mc = new MathContext(128, RoundingMode.HALF_UP);
        BigDecimal zx = BigDecimal.ZERO;
        BigDecimal zy = BigDecimal.ZERO;
        BigDecimal two = new BigDecimal("2");
        for (int i = 1; i < maxIter; i++) {
            BigDecimal nzx = zx.multiply(zx, mc).subtract(zy.multiply(zy, mc), mc).add(centerX);
            BigDecimal nzy = zx.multiply(zy, mc).multiply(two, mc).add(centerY, mc);
            orbitValues[i*2] = nzx.floatValue();
            orbitValues[i*2+1] = nzy.floatValue();
            if (Float.isInfinite(orbitValues[i*2]) && Float.isInfinite(orbitValues[i*2 + 1])) {
                float vx = orbitValues[i*2];
                float vy = orbitValues[i*2+1];
                for (;i < maxIter; i++) {
                    orbitValues[i*2] = vx;
                    orbitValues[i*2+1] = vy;
                }
            }
            zx = nzx;
            zy = nzy;
            System.out.print("\r"+(i+1)+"/"+maxIter);
        }
        System.out.println();
        System.out.println("Calculated reference orbit");
    }

    private void initGLFWWindow() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        System.out.println("Init successful");

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);

        window = GLFW.glfwCreateWindow(128, 128, "Window", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        System.out.println("Window creation successful");

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwShowWindow(window);
    }

    private int loadShader(String path, int type) throws Exception {
        int res = glCreateShader(type);
        glShaderSource(res, Files.readString(Path.of(path)));
        glCompileShader(res);
        if (glGetShaderi(res, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new Exception(String.format(
                "Failed to compile vertex shader!\n\t%s",
                glGetShaderInfoLog(res)
            ));
        }
        return res;
    }

    private void initProgram() throws Exception {
        GL.createCapabilities();
        System.out.println("Created capabilities");

        int vert = loadShader("res/quad.vert.glsl", GL_VERTEX_SHADER);
        int frag = loadShader("res/quad.frag.glsl", GL_FRAGMENT_SHADER);

        System.out.println("Loaded shaders");

        program = glCreateProgram();
        glAttachShader(program, vert);
        glAttachShader(program, frag);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new Exception(String.format(
                "Failed to link shader program!\n\t%s",
                glGetProgramInfoLog(program)
            ));
        }

        glDeleteShader(vert);
        glDeleteShader(frag);

        glUseProgram(program);
        System.out.println("Created program");
    }

    private void initVertices() {
        float[] vertices = new float[]{
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
            1.0f,  1.0f, 0.0f
        };

        int[] indices = new int[]{
            0, 1, 2,
            1, 2, 3
        };

        // Vertex Buffer Object
        int VBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // Index Buffer Object
        int IBO = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, IBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        // Vertex Array Object
        VAO = glGenVertexArrays();
        glBindVertexArray(VAO);
        int posLocation = glGetAttribLocation(program, "a_pos");
        glEnableVertexAttribArray(posLocation);

        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glVertexAttribPointer(posLocation, 3, GL_FLOAT, false, 0, 0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, IBO);
        glBindVertexArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        System.out.println("initialized vertices");
    }

    private void initUniforms() {
        glUniform2fv(
            glGetUniformLocation(program, "res"),
            new float[]{width, height}
        );

        glUniform1i(
            glGetUniformLocation(program, "aaLevel"),
            aaLevel
        );

        glUniform1f(
            glGetUniformLocation(program, "mag"),
            mag
        );

        glUniform1i(
            glGetUniformLocation(program, "maxIter"),
            maxIter
        );

        int orbitValueBuffer = glGenBuffers();
        glBindBuffer(GL_TEXTURE_BUFFER, orbitValueBuffer);
        glBufferData(GL_TEXTURE_BUFFER, orbitValues, GL_STATIC_READ);
        glBindBuffer(GL_TEXTURE_BUFFER, 0);

        int orbitValueTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, orbitValueTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_RG32F, orbitValueBuffer);
        glActiveTexture(GL_TEXTURE_BUFFER);
    }

    private void render() {
        int RBO = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, RBO);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA, width, height);

        int FBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, RBO);
        glDrawBuffer(GL_FRAMEBUFFER);

        glViewport(0, 0, width, height);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glBindVertexArray(VAO);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        GLFW.glfwSwapBuffers(window);
        GLFW.glfwPollEvents();
    }

    private void save() {
        glReadBuffer(GL_FRAMEBUFFER);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width*height*4);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        int x, y, idx;
        for (x = 0; x < width; x++) {
            for (y = 0; y < height; y++) {
                idx = (x+width*y);
                pixels[(x + width*(height-y-1))] = 0xff000000 |
                    ( buffer.get(4*idx  )         << 16) |
                    ((buffer.get(4*idx+1) & 0xff) <<  8) |
                    ( buffer.get(4*idx+2) & 0xff);
            }
        }

        try {
            //noinspection ResultOfMethodCallIgnored
            new File("out/").mkdirs();
            ImageIO.write(image, "png", new File("out/out.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}