package com.example.myapplication.engine;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Cube {

    private final float[] vertices = {
            // X, Y, Z, R, G, B
            -1, -1, -1, 1, 0, 0,
            1, -1, -1, 0, 1, 0,
            1,  1, -1, 0, 0, 1,
            -1,  1, -1, 1, 1, 0,
            -1, -1,  1, 1, 0, 1,
            1, -1,  1, 0, 1, 1,
            1,  1,  1, 1, 1, 1,
            -1,  1,  1, 0, 0, 0
    };

    private final short[] indices = {
            0,1,2, 0,2,3,      // back
            4,5,6, 4,6,7,      // front
            0,1,5, 0,5,4,      // bottom
            3,2,6, 3,6,7,      // top
            1,2,6, 1,6,5,      // right
            0,3,7, 0,7,4       // left
    };

    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;

    private int program;

    public Cube() {
        // Allocate buffers
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        indexBuffer = ByteBuffer.allocateDirect(indices.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        indexBuffer.put(indices).position(0);

        // Create shaders
        String vertexShaderCode =
                "uniform float uAngle;" +
                        "attribute vec3 aPos;" +
                        "attribute vec3 aColor;" +
                        "varying vec3 vColor;" +
                        "void main() {" +
                        "  float s = sin(radians(uAngle));" +
                        "  float c = cos(radians(uAngle));" +
                        "  mat4 rot = mat4(" +
                        "    c, 0, s, 0," +
                        "    0, 1, 0, 0," +
                        "   -s, 0, c, 0," +
                        "    0, 0, 0, 1);" +
                        "  gl_Position = rot * vec4(aPos, 1.0);" +
                        "  vColor = aColor;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "varying vec3 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vec4(vColor, 1.0);" +
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
    }

    public void draw(float angle) {
        GLES20.glUseProgram(program);

        int pos = GLES20.glGetAttribLocation(program, "aPos");
        int color = GLES20.glGetAttribLocation(program, "aColor");
        int uAngle = GLES20.glGetUniformLocation(program, "uAngle");

        GLES20.glUniform1f(uAngle, angle);

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 6 * 4, vertexBuffer);
        GLES20.glEnableVertexAttribArray(pos);

        vertexBuffer.position(3);
        GLES20.glVertexAttribPointer(color, 3, GLES20.GL_FLOAT, false, 6 * 4, vertexBuffer);
        GLES20.glEnableVertexAttribArray(color);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }
}