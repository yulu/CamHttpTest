package com.research.camhttptest.cv;

import com.research.camhttptest.gl.*;

import java.io.IOException;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;

import com.research.camhttptest.gl.CamData;


public class CvFrameProcess implements PreviewCallback, GLRenderer.CvFrameProcessListener{
		
		private static final String TAG = "CvFrameProcess";
		private static final int MAX_UNSPECIFIED = -1;
		private static final int STOPPED = 0;
		private static final int STARTED = 1;

		/**
		 * Constructors of the class---------------------------------------------
		 */
		public CvFrameProcess() {
			mMaxWidth = MAX_UNSPECIFIED;
			mMaxHeight = MAX_UNSPECIFIED;
		}
		

		@Override
		public void onFrameProcessStarted(int width, int height) {
			disableView();
			setDimension(width, height);
			enableView();
			
		}

		@Override
		public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture) {
			try{		
				setPreviewTexture(surfaceTexture);
				enableView();
			}catch(final Exception ex){
				
			}
			
		}
		
	    public void enableView() {
	        synchronized(this) {
	            mEnabled = true;
	            checkCurrentState();
	        }
	    }
	    
	    public void disableView() {
	        synchronized(this) {
	            mEnabled = false;
	            checkCurrentState();
	        }
	    }
	    
	    public void setCvFrame(CamData mCamData){
	    	this.mCamData = mCamData;
	    }
	    
	    public void setPreviewTexture(SurfaceTexture surfaceTexture) throws IOException{
	    	mSurfaceTexture = surfaceTexture;
	    	mCamera.setPreviewTexture(surfaceTexture);
	    }
	    
	    public void setDimension(int width, int height){
	    	mSurfaceWidth = width;
	    	mSurfaceHeight = height;
	    }
		
	    /**
	     * state related
	     */
		private void checkCurrentState(){
			int targetState;
			
			if(mEnabled){
				targetState = STARTED;
			}else{
				targetState = STOPPED;
			}
			
			if(targetState != mState){
				/* The state change detected. Need to exit the current state and enter
				 * target state
				 */
				processExitState(mState);
				mState = targetState;
				processEnterState(mState);
			}
		}
		
		private void processEnterState(int state){
			switch(state){
			case STARTED:
				onEnterStartedState();
				break;
			case STOPPED:
				onEnterStoppedState();
				break;
			};
		}
		
		private void processExitState(int state){
			switch(state){
			case STARTED:
				onExitStartedState();
				break;
			case STOPPED:
				onExitStoppedState();
				break;
			};
		}
		
		
		private void onEnterStoppedState(){
			//do nothing
		}
		private void onExitStoppedState(){
			//do nothing
		}
		
		private void onEnterStartedState(){
			/*
			 * Connect camera
			 */
			if(!connectCamera(mSurfaceWidth, mSurfaceHeight)){
				Log.e(TAG, "cannot connect to camera");
			}
		}
		
		private void onExitStartedState(){
			/*
			 * disconnect camera
			 */
			disconnectCamera();
		}
		
		/*-----------------------------------------------------------------*/
		
		/**
		 * Camera setup -----------------------------------------------------
		 */
		private boolean connectCamera(int width, int height){
			/*
			 * 1. We need to instantiate camera
			 * 2. We need to start thread which will be getting frames
			 */
			Log.d(TAG, "Connecting to camera");
			if(!initializeCamera(width, height))
				return false;
			
			/* now we can start update thread*/
			Log.d(TAG, "Starting processing thread");
			mStopThread = false;
			mThread = new Thread(new CameraWorker());
			mThread.start();
			
			return true;
		}
		
		private void disconnectCamera(){
			/*
			 * 1. We need to stop thread which updating the frames
			 * 2. Stop camera and release it
			 */
			Log.d(TAG, "Disconnecting from camera");
			try{
				mStopThread = true;
				Log.d(TAG, "Notify thread");
				synchronized(this){
					this.notify();
				}
				Log.d(TAG, "Waiting for thread");
				if(mThread != null)
					mThread.join();
			}catch(InterruptedException e){
				e.printStackTrace();
			}finally{
				mThread = null;
			}
			
			/* Now release the camera */
			releaseCamera();
		}
		
		@SuppressLint("NewApi")
		private boolean initializeCamera(int width, int height){
			Log.d(TAG, "Initialize java camera");
			boolean result = true;
			synchronized(this){
				mCamera = null;
				/**
				 * Open Camera--------------------------
				 */
				Log.i(TAG, "Trying to open back camera");
				int localCameraIndex = -1;
				Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
				for(int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx){
					Camera.getCameraInfo(camIdx, cameraInfo);
					if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
						localCameraIndex = camIdx;
						break;
					}
				}
				if(localCameraIndex != -1){
					try{
						mCamera = Camera.open(localCameraIndex);
					}catch(RuntimeException e){
						Log.e(TAG, "Camera #" + localCameraIndex + "failed to open: " +
					e.getLocalizedMessage());
					}
				}else{
					Log.e(TAG, "Back camera not found!");
				}
				
				if(mCamera == null)
					return false;
				
				/**
				 * set camera parameters------------------------------
				 */
				try{
					//size
					Camera.Parameters params = mCamera.getParameters();
					Log.d(TAG, "getSupportedPreviewSizes()");
					List<android.hardware.Camera.Size> sizes = params.getSupportedPreviewSizes();
					
					if(sizes != null){
						/* Select the size that fits surface considering maximum size allowed*/
						Size frameSize = calculateCameraFrameSize(sizes, new CameraSizeAccessor(),
								width, height);
						params.setPreviewFormat(ImageFormat.NV21);
						Log.d(TAG, "Set preview size to "+Integer.valueOf((int)frameSize.width) + "x" +
						Integer.valueOf((int)frameSize.height));
						params.setPreviewSize((int)frameSize.width, (int)frameSize.height);					
					
					
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
							params.setRecordingHint(true);
						}
					
						//focus
						List<String> FocusModes = params.getSupportedFocusModes();
						if(FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
							params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
						}
					
						mCamera.setParameters(params);
						params = mCamera.getParameters();
					
						mFrameWidth = params.getPreviewSize().width;
						mFrameHeight = params.getPreviewSize().height;
					
						//byte buffer
						int size = mFrameWidth * mFrameHeight;
						size = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat())/8;
						mBuffer = new byte[size];
					
						mCamera.addCallbackBuffer(mBuffer);
						mCamera.setPreviewCallbackWithBuffer(this);
					
						mFrameChain = new Mat[2];
						mFrameChain[0] = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);
						mFrameChain[1] = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);
					
						
						mCameraFrame = new CameraFrame[2];
						mCameraFrame[0] = new CameraFrame(mFrameChain[0], mFrameWidth, mFrameHeight);
						mCameraFrame[1] = new CameraFrame(mFrameChain[1], mFrameWidth, mFrameHeight);
						
						//orientation and aspect ratio					
						float orientationM[] = new float[16];
						Matrix.setRotateM(orientationM, 0, 0.0f, 0f, 0f, 1f);
						mCamData.setOrientationM(orientationM);
						
						float aspectRatio[] = new float[2];
						aspectRatio[0] = (float)Math.min(mFrameWidth, mFrameHeight)/mFrameWidth;
						aspectRatio[1] = (float)Math.min(mFrameWidth, mFrameHeight)/mFrameHeight;					
						mCamData.setAspectRatio(aspectRatio[0] , aspectRatio[1] );
								
						//set surface texture
						if(mSurfaceTexture != null)
							mCamera.setPreviewTexture(mSurfaceTexture);
					
						/*Finally we are ready to start the preview*/
						Log.d(TAG, "startPreview");
						mCamera.startPreview();
						
					}else
						result = false;
					
				}catch(Exception e){
					result = false;
					e.printStackTrace();
				}
				return result;
			}
		}
		
		private void releaseCamera(){
			synchronized(this){
				if(mCamera != null){
					mCamera.stopPreview();
					mCamera.setPreviewCallback(null);
					
					mCamera.release();
				}
				mCamera = null;
				if(mFrameChain != null){
					mFrameChain[0].release();
					mFrameChain[1].release();
				}
				if(mCameraFrame != null){
					mCameraFrame[0].release();
					mCameraFrame[1].release();
				}
			}
		}
		
		/**
		 * PreviewCallback interface method implementation
		 */
		@Override
		public void onPreviewFrame(byte[] frame, Camera arg1) {
			Log.d(TAG, "Preview Frame received. Frame size: " + frame.length);
			synchronized(this){
				mFrameChain[1-mChainIdx].put(0, 0, frame);
				this.notify();
			}
			if(mCamera != null)
				mCamera.addCallbackBuffer(mBuffer);
		}
		
		public void takePicture(FrameDataCallback mFrameDataCallback){
			//synchronized(this){
				if(mFrameDataCallback != null && mCameraFrame[1-mChainIdx] != null) 
					mFrameDataCallback.onFrameDataReceived(mCameraFrame[1-mChainIdx], mFrameWidth, mFrameHeight);
			//}
			
		}
		
		/**
		 * Draw camera view on the canvas--------------------------------
		 * @author yulu
		 *
		 */
		
		private class CameraWorker implements Runnable{
			public void run(){
				do{
					synchronized (CvFrameProcess.this){
						try{
							CvFrameProcess.this.wait();
						}catch(InterruptedException e){
							e.printStackTrace();
						}
					}
					
					if(!mStopThread){
						if(!mFrameChain[mChainIdx].empty())
							//draw the frame on the surface canvas
							deliverFrame(mCameraFrame[mChainIdx]);
						mChainIdx = 1 - mChainIdx;
					}
				}while(!mStopThread);
				Log.d(TAG, "Finish processing thread");
			}
		}
		
		private void deliverFrame(CameraFrame frame){
			//TODO: process image
			Mat mIntermediateMat = new Mat(frame.gray().rows(), frame.gray().cols(), CvType.CV_8UC1);
			Imgproc.Canny(frame.gray(), mIntermediateMat, 80, 100);
			
			/**
			 * NOTE:
			 * The tracking algorithm can be called here,  
			 * output the projection and render flag to mCamData object, which is shared with the OpenGL rendering class.
			 */
			
		}
		
		public interface FrameDataCallback{
			public void onFrameDataReceived(CameraFrame frame, int mFrameWidth, int mFrameHeight);
		}
		
		
		public static class CameraSizeAccessor implements ListItemAccessor {

	        public int getWidth(Object obj) {
	            Camera.Size size = (Camera.Size) obj;
	            return size.width;
	        }

	        public int getHeight(Object obj) {
	            Camera.Size size = (Camera.Size) obj;
	            return size.height;
	        }
	    }
		
		
	    public interface ListItemAccessor {
	        public int getWidth(Object obj);
	        public int getHeight(Object obj);
	    };
		
		protected Size calculateCameraFrameSize(List<?> supportedSizes, ListItemAccessor accessor, int surfaceWidth, int surfaceHeight) {
	        int calcWidth = 0;
	        int calcHeight = 0;

	        int maxAllowedWidth = (mMaxWidth != MAX_UNSPECIFIED && mMaxWidth < surfaceWidth)? mMaxWidth : surfaceWidth;
	        int maxAllowedHeight = (mMaxHeight != MAX_UNSPECIFIED && mMaxHeight < surfaceHeight)? mMaxHeight : surfaceHeight;

	        for (Object size : supportedSizes) {
	            int width = accessor.getWidth(size);
	            int height = accessor.getHeight(size);

	            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
	                if (width >= calcWidth && height >= calcHeight) {
	                    calcWidth = (int) width;
	                    calcHeight = (int) height;
	                }
	            }
	        }

	        return new Size(calcWidth, calcHeight);
	    }

		
		/**
		 * Camera related
		 */
		private Camera mCamera;
		private CameraFrame[] mCameraFrame;
		private CamData	mCamData;
		private SurfaceTexture mSurfaceTexture;
		private byte mBuffer[];
		private Mat[] mFrameChain;
		
		private int mFrameWidth;
		private int mFrameHeight;
		private int mMaxWidth;
		private int mMaxHeight;
		private int mSurfaceWidth;
		private int mSurfaceHeight;
		
		/**
		 * State related
		 */
		private int mState = STOPPED;
		private boolean mEnabled;
		
		/**
		 * Thread related
		 */
		private int mChainIdx = 0;
		private Thread mThread;
		private boolean mStopThread;
		
	}
