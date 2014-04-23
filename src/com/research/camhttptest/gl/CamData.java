package com.research.camhttptest.gl;

public class CamData {
	
	public CamData(){
		super();
	}
	
	public CamData(float[] aspectRatio, float[] orientationM){
		super();
		this.mAspectRatioPreview = aspectRatio;
		this.mOrientationM = orientationM;
	}
	
	public float[] aspectRatio(){
		return mAspectRatioPreview;
	}
	
	
	public float[] orientationM(){
		return mOrientationM;
	}
	
	public void setOrientationM(float M[]){
		mOrientationM =  M;
	}
	
	public void setAspectRatio(float r1, float r2){
		mAspectRatioPreview[0] = r1;
		mAspectRatioPreview[1] = r2;
	}
	
	public void setRenderFlag(boolean on){
		mRenderFlag = on;
	}
	public boolean getRenderFlag(){
		return mRenderFlag;
	}
	
	//preview aspect ration
	private float mAspectRatioPreview[] = new float[2];
	//camera orientation matrix
	private float mOrientationM[] = new float[16];
	//projection matrix
	private float mProjectionM[] = new float[16];
	//render on flag
	private boolean mRenderFlag = false;
}
