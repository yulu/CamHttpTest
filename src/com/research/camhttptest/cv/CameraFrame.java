package com.research.camhttptest.cv;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class CameraFrame {
	public CameraFrame(Mat Yuv420sp, int width, int height){
		super();
		mWidth = width;
		mHeight = height;
		mYuvFrameData = Yuv420sp;
		mRgba = new Mat();
	}
	
	public void release(){
		mRgba.release();
	}
	
	public Mat gray(){
		return mYuvFrameData.submat(0, mHeight, 0, mWidth);
	}
	
	public Mat rgba(){
		Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2BGR_NV12, 4);
		return mRgba;
	}
	
	
	private Mat mYuvFrameData;
	private Mat mRgba;
	private int mWidth;
	private int mHeight;
}
