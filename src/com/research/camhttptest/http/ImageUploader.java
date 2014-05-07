package com.research.camhttptest.http;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.opencv.android.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;

import com.research.camhttptest.cv.CameraFrame;
import com.research.camhttptest.cv.CvFrameProcess;
import com.research.camhttptest.cv.CvFrameProcess.FrameDataCallback;
import com.research.camhttptest.http.AsyncClientRunner.RequestListener;

public class ImageUploader implements RequestListener, FrameDataCallback{
	/**
	 * Http Client
	 */
	private Client client;
	private AsyncClientRunner asyncrunner;
	
	/**
	 * Image byte array
	 */
	private byte[] imageByteArray;
	
	/**
	 * Interface
	 */
	private OnImageUploadedListener mOnImageUploadedListener;
	
	public interface OnImageUploadedListener{
		public void onImageUploaded(String result);
	}
	
	public void setImageUploadedListener(OnImageUploadedListener mOnImageUploadedListener){
		this.mOnImageUploadedListener = mOnImageUploadedListener;
	}

	public ImageUploader() {
		client = Client.getInstance();
		asyncrunner = new AsyncClientRunner(client);
	}
	
	/**
	 * asyncUploader: capture the current frame and uploaded to the server (in a background thread)
	 * @param context
	 * @param cvFrameProcess
	 * @param longitude
	 * @param latitude
	 */
	public void asyncUploader(final Context context,
			final CvFrameProcess cvFrameProcess, final float longitude, final float latitude){
		
		new Thread(){
			@Override
			public void run(){
				uploadFrame(context, cvFrameProcess, longitude, latitude);
			}
		}.start();
	}
	
	private void uploadFrame(Context context, CvFrameProcess cvFrameProcess, float longitude, float latitude){
		synchronized(this){
			cvFrameProcess.takePicture(this);
			
			String[] identifier = {"image_byte_array", "longitude", "latitude", "location_name", "object_name"};
			String[] value = {"location1", "object1"};
			
			Bundle params = new Bundle();
			params.putByteArray(identifier[0], imageByteArray);
			params.putString(identifier[1], String.valueOf(longitude));
			params.putString(identifier[2], String.valueOf(latitude));
			params.putString(identifier[3], value[0]);
			params.putString(identifier[4], value[1]);
			
			asyncrunner.request(context, 
					HttpConstant.UPLOAD_FOR_RECO_URI, 
					params, 
					Client.HTTPMETHOD_POST, 
					"file", 
					this, 
					"upload image");
		
			this.notify();
		}

	}

	@Override
	public void onComplete(String response, Object state) {
		final String result = JsonParser.ClassParser(response);
		if(mOnImageUploadedListener != null){
			mOnImageUploadedListener.onImageUploaded(result);
		}
		
	}

	@Override
	public void onComplete(byte[] response, Object state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onIOException(IOException e, Object state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFileNotFoundException(FileNotFoundException e, Object state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMalformedURLException(MalformedURLException e, Object state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFrameDataReceived(CameraFrame frame, int width, int height) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
  		synchronized(this){
  			if (frame != null){ 			 			
				Bitmap myImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565); 
				Utils.matToBitmap(frame.gray(), myImage);

				myImage.compress(CompressFormat.PNG, 75, stream);
				imageByteArray = stream.toByteArray();			
  			}

  			this.notify();
		}
		
	}

}
