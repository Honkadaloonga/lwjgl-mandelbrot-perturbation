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
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Main {
    private static final int width = 1280;
    private static final int height = 720;

    private final long window;

    private Main() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        System.out.println("Init successful");

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);

        window = GLFW.glfwCreateWindow(width, height, "Window", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        System.out.println("Window creation successful");

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwShowWindow(window);
    }

    private void run() throws Exception {
        GL.createCapabilities();
        System.out.println("Created capabilities");

        int frag, vert, shader;

        vert = loadShader("res/quad.vert.glsl", GL_VERTEX_SHADER);
        System.out.println("Loaded vertex shader");
        frag = loadShader("res/quad.frag.glsl", GL_FRAGMENT_SHADER);
        System.out.println("Loaded fragment shader");

        shader = glCreateProgram();
        System.out.println("Created program");
        glAttachShader(shader, vert);
        glAttachShader(shader, frag);
        System.out.println("Attached shaders");
        glLinkProgram(shader);

        if (glGetProgrami(shader, GL_LINK_STATUS) == GL_FALSE) {
            throw new Exception(String.format(
                "Failed to link shader program!\n\t%s",
                glGetProgramInfoLog(shader)
            ));
        }
        System.out.println("Linked program");

        glDeleteShader(vert);
        glDeleteShader(frag);
        System.out.println("Deleted shaders");

        glUseProgram(shader);
        System.out.println("Using program");

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
        int VAO = glGenVertexArrays();
        glBindVertexArray(VAO);
        int posLocation = glGetAttribLocation(shader, "a_pos");
        glEnableVertexAttribArray(posLocation);

        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glVertexAttribPointer(posLocation, 3, GL_FLOAT, false, 0, 0);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, IBO);
        glBindVertexArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        System.out.println("initialized buffers");

        glUniform2fv(
            glGetUniformLocation(shader, "center"),
            new float[]{
                -0.214510536082075f,
                0.69998295217564f
            }
        );

        glUniform2fv(
            glGetUniformLocation(shader, "res"),
            new float[]{width, height}
        );

        glUniform1f(
            glGetUniformLocation(shader, "mag"),
            1000000.0f
        );

        while (!GLFW.glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glBindVertexArray(VAO);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();

            glReadBuffer(GL_FRONT);
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
                ImageIO.write(image, "png", new File("out/out.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            GLFW.glfwSetWindowShouldClose(window, true);
        }

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
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

    public static void main(String[] args) {
        System.out.println("Hello world!");

        try {
            new Main().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}