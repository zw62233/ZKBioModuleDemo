package com.armatura.biomodule.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.AttributeSet;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PreviewView extends GLSurfaceView {

    final BitmapRenderer bitmapRenderer = new BitmapRenderer();
    private final Queue<Bitmap> bitmapQueue = new ArrayBlockingQueue<>(2);

    public PreviewView(Context context) {
        super(context);
        init();
    }

    public PreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void addFrame(Bitmap bitmap) {
        boolean boffer = bitmapQueue.offer(bitmap);
        if (!boffer) {
            Bitmap bmp = bitmapQueue.poll();
            Log.i("PreviewView", "Drop bimtap draw");
            if (!bmp.isRecycled()) {
                bmp.recycle();
            }
            bitmapQueue.offer(bitmap);
        }
        requestRender();
    }

    public void init() {
        setEGLContextClientVersion(1);
        setRenderer(bitmapRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private class BitmapRenderer implements GLSurfaceView.Renderer {

        private final float[] VERTEX_COORDINATES = new float[]{
                -1.0f, +1.0f, 0.0f,
                +1.0f, +1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                +1.0f, -1.0f, 0.0f
        };
        private final float[] TEXTURE_COORDINATES = new float[]{
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f
        };
        private final Buffer TEXCOORD_BUFFER = ByteBuffer.allocateDirect(TEXTURE_COORDINATES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer().put(TEXTURE_COORDINATES).rewind();
        private final Buffer VERTEX_BUFFER = ByteBuffer.allocateDirect(VERTEX_COORDINATES.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer().put(VERTEX_COORDINATES).rewind();
        private int[] textures;
        BitmapRenderer() {

        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            textures = new int[1];
            gl.glEnable(GL10.GL_TEXTURE_2D);
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

            gl.glGenTextures(1, textures, 0);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

            //GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher), 0);
            gl.glActiveTexture(GL10.GL_TEXTURE0);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, VERTEX_BUFFER);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, TEXCOORD_BUFFER);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (bitmapQueue.isEmpty()) {
                return;
            }
            Bitmap bitmap = bitmapQueue.poll();
            Log.d("PreviewView", String.format("------------------showBitmap\t%d", bitmapQueue.size()));
            if (bitmap == null || bitmap.isRecycled()) {
                return;
            }

            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

            bitmap.recycle();
        }
    }

}
