package com.research.camhttptest.gl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.research.camhttptest.R;


public class GLRenderer extends GLSurfaceView implements 
				GLSurfaceView.Renderer, OnFrameAvailableListener
{

	/**
	 * External OES texture holder, camera preview
	 */
	private final CamFBO mFBOExternal = new CamFBO();
	private final CamFBO mFBOOffscreen = new CamFBO();
	
	private final CamShader mShaderCopyOes = new CamShader();
	private final CamShader shader = new CamShader();

	public GLRenderer(Context context) {
		super(context);
		init();
	}
	
	public GLRenderer(Context context, AttributeSet attrs){
		super(context, attrs);
		init();
	}
	
	public interface CvFrameProcessListener{
		/**
		 * deliver the width and height
		 */
		public void onFrameProcessStarted(int width, int height);
		
		/**
		 * pass the surfacetexture
		 */
		public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture);
	}
	
	
	/**
	 * Initializes local variables for rendering------------------------------------------
	 */
	@SuppressLint("NewApi")
	private void init(){
		final byte FULL_QUAD_COORDS[] = {-1, 1, -1, -1, 1, 1, 1, -1};
		mFullQuadVertices = ByteBuffer.allocateDirect(4*2);
		mFullQuadVertices.put(FULL_QUAD_COORDS).position(0);
		
		setPreserveEGLContextOnPause(true);
		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
	public void setCvFrame(CamData mCamData){
		this.mCamData = mCamData;
		//requestRender();
	}
	
	public void setCvFrameProcessListener(CvFrameProcessListener mCvFrameProcessListener){
		this.mCvFrameProcessListener = mCvFrameProcessListener;
	}
	
	/**
	 * OpenGL related methods------------------------------------------------------------------
	 */

	@SuppressLint("NewApi")
	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
		//mark a flag for indicating new frame is available
		mSurfaceTextureUpdate = true;
		requestRender();
		
	}

	@SuppressLint("NewApi")
	@Override
	public synchronized void onDrawFrame(GL10 arg0) {
		//clear view
		GLES20.glClearColor(.5f, .5f, .5f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		
		//if new preview if available
		if(mSurfaceTextureUpdate){
			//update surface texture
			mSurfaceTexture.updateTexImage();
			mSurfaceTexture.getTransformMatrix(mTransformM);
			mSurfaceTextureUpdate = false;
			
			//bind offscreen texture into use
			mFBOOffscreen.bind();
			mFBOOffscreen.bindTexture(0);
			
			//take copy shader into use
			mShaderCopyOes.useProgram();
			
			//Uniform variables
			int uOrientationM = mShaderCopyOes.getHandle("uOrientationM");
			int uTransformM = mShaderCopyOes.getHandle("uTransformM");
			
			//Transform external texture
			GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mCamData.orientationM(), 0);
			GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);
			
			//Using external OES texture as source
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFBOExternal.getTexture(0));
			
			renderQuad(mShaderCopyOes.getHandle("aPosition"));
		}
		
		//bind screen buffer into use
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mFrameWidth, mFrameHeight);
		
		shader.useProgram();
		
		int uAspectRatio = shader.getHandle("uAspectRatio");
		int uAspectRatioPreview = shader.getHandle("uAspectRatioPreview");
		
		GLES20.glUniform2fv(uAspectRatio, 1, mAspectRatio, 0);
		GLES20.glUniform2fv(uAspectRatioPreview, 1, mCamData.aspectRatio(), 0);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFBOOffscreen.getTexture(0));
		
		int uPixelSize = shader.getHandle("uPixelSize");
		GLES20.glUniform2f(uPixelSize, 1.0f/mFrameWidth, 1.0f/mFrameHeight);
		
		//Trigger actual rendering
		renderQuad(mShaderCopyOes.getHandle("aPosition"));
		
	}

	@SuppressLint("NewApi")
	@Override
	public synchronized void onSurfaceChanged(GL10 arg0, int width, int height) {

		
		mFrameWidth = width;
		mFrameHeight = height;
		
		//Get aspect ratio
		mAspectRatio[0] = (float)Math.min(mFrameWidth, mFrameHeight)/mFrameWidth;
		mAspectRatio[1] = (float)Math.min(mFrameWidth, mFrameHeight)/mFrameHeight;
		
		//Init texture
		if(mFBOExternal.getWidth() != mFrameWidth || mFBOExternal.getHeight() != mFrameHeight){
			mFBOExternal.init(mFrameWidth, mFrameHeight, 1, true);
		}
		if(mFBOOffscreen.getWidth() != mFrameWidth || mFBOOffscreen.getHeight() != mFrameHeight){
			mFBOOffscreen.init(mFrameWidth, mFrameHeight, 1, false);
		}
		
		//Allocate new SurfaceTexture
		SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
		mSurfaceTexture = new SurfaceTexture(mFBOExternal.getTexture(0));
		mSurfaceTexture.setOnFrameAvailableListener(this);
		
		if(mCvFrameProcessListener != null){
			mCvFrameProcessListener.onFrameProcessStarted(width, height);
			mCvFrameProcessListener.onSurfaceTextureCreated(mSurfaceTexture);
		}
		
		
		if(oldSurfaceTexture != null){
			oldSurfaceTexture.release();
		}
		
		requestRender();
	}

	@Override
	public synchronized void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
		try{
			String vertexSource = loadRawString(R.raw.copy_oes_vs);
			String fragmentSource = loadRawString(R.raw.copy_oes_fs);
			mShaderCopyOes.setProgram(vertexSource, fragmentSource);
		}catch(Exception ex){
			ex.printStackTrace();
		}

		
		try{		
			String vs = loadRawString(R.raw.filter_vs);
			String fs = loadRawString(R.raw.filter_fs);
			shader.setProgram(vs, fs);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		mFBOExternal.reset();
		mFBOOffscreen.reset();
	}
	

	
	/**
	 * Utilities----------------------------------------------------------------
	 */
	private String loadRawString(int rawId) throws Exception{
		InputStream is = getContext().getResources().openRawResource(rawId);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int len;
		while((len = is.read(buf))!= -1){
			baos.write(buf, 0, len);
		}
		return baos.toString();
	}
	
	private void renderQuad(int aPosition){
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0, mFullQuadVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}
	
	
	private CvFrameProcessListener mCvFrameProcessListener;
	
	private CamData mCamData;
	private SurfaceTexture mSurfaceTexture;
	
	private int mFrameWidth;
	private int mFrameHeight;
	/**
	 * OpenGL related
	 */
	private float mAspectRatio[] = new float[2];
	private float mTransformM[] = new float[16];
	private ByteBuffer mFullQuadVertices;
	
	/**
	 * state related
	 */
	private boolean mSurfaceTextureUpdate;	

}

