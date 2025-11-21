package com.example.myapplication.engine;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class MyGLSurfaceView extends GLSurfaceView {

    public MyGLSurfaceView(Context context) {
        super(context);

        setEGLContextClientVersion(2);   // OpenGL ES 2.0
        setRenderer(new CubeRenderer()); // our renderer
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}