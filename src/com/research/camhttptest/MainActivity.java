package com.research.camhttptest;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.research.camhttptest.cv.CvFrameProcess;
import com.research.camhttptest.gl.CamData;
import com.research.camhttptest.gl.GLRenderer;
import com.research.camhttptest.http.HttpConstant;
import com.research.camhttptest.http.ImageUploader;

public class MainActivity extends Activity implements LocationListener{
	
	/**
	 * CV and GL related
	 */
	private GLRenderer				mGLRenderer;
	private CvFrameProcess			mCvFrameProcess = new CvFrameProcess();
	private CamData					mCamData = new CamData();
	
	/**
	 * GPS related
	 */
    private LocationManager mLocationManager;
    private TextView longView;
    private TextView latView;
    private float latitude;
    private float longitude;

	private Button mButton;
	
	//image uploader and image downloader
	private ImageUploader mImageUploader;
	protected ImageLoader imageLoader = ImageLoader.getInstance();
	private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();
	private DisplayImageOptions options;
	private ImageView mImageView;
	
	/**
	 * Image byte array
	 */
    
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this){
		@Override
		public void onManagerConnected(int status){
			switch(status){
				case LoaderCallbackInterface.SUCCESS:
				{				
					//Load native library after OpenCV initialization
					//System.loadLibrary("feature");
					
					//mCameraView.enableView();
					mCvFrameProcess.enableView();
				}break;
				default:
				{
					super.onManagerConnected(status);
				}break;
			}
		}
	};
	
	public MainActivity(){

	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.activity_main);
		
		mCvFrameProcess.setCvFrame(mCamData);
		mGLRenderer = (GLRenderer)findViewById(R.id.cam_view);
		mGLRenderer.setCvFrame(mCamData);
		mGLRenderer.setCvFrameProcessListener(mCvFrameProcess);
		
		//image loader options
		options = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.ic_launcher)
				.showImageForEmptyUri(R.drawable.ic_launcher)
				.showImageOnFail(R.drawable.ic_launcher)
				.cacheInMemory(true)
				.cacheOnDisc(true)
				.displayer(new RoundedBitmapDisplayer(20))
				.build();
		
		//init ImageUploader
		mImageView = (ImageView)findViewById(R.id.object_image);
		mImageUploader = new ImageUploader();
		mImageUploader.setImageUploadedListener(new ImageUploader.OnImageUploadedListener() {
			
			@Override
			public void onImageUploaded(String result) {
				final String f_str = result;
				runOnUiThread(new Runnable(){
			        public void run() {
			            Toast.makeText(getApplicationContext(), f_str, Toast.LENGTH_LONG).show();
						imageLoader.displayImage(HttpConstant.DOWNLOAD_IMG_URI+f_str, mImageView, options, animateFirstListener);
			        }
			    });
				
				
			}
		});
		
        longView = (TextView)findViewById(R.id.longitude);
        latView = (TextView)findViewById(R.id.latitude);
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        
		mButton = (Button)findViewById(R.id.button);
		mButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	mImageUploader.asyncUploader(getApplicationContext(), mCvFrameProcess, longitude, latitude);
            }
        });
	}
	
    @Override
    public void onResume(){
    	super.onResume();
    	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    	
    	mCvFrameProcess.enableView();
    	mGLRenderer.onResume();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mCvFrameProcess.disableView();
        mLocationManager.removeUpdates(this);
    }

	@Override
	public void onLocationChanged(Location location) {
		latitude = (float) (location.getLatitude());
        longitude = (float) (location.getLongitude());
        
        longView.setText(String.valueOf(longitude));
        latView.setText(String.valueOf(latitude));
		
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
	
	private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener {

		static final List<String> displayedImages = Collections.synchronizedList(new LinkedList<String>());

		@Override
		public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
			if (loadedImage != null) {
				ImageView imageView = (ImageView) view;
				boolean firstDisplay = !displayedImages.contains(imageUri);
				if (firstDisplay) {
					FadeInBitmapDisplayer.animate(imageView, 500);
					displayedImages.add(imageUri);
				}
			}
		}
	}



}
