package com.research.camhttptest;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.research.camhttptest.cv.CameraFrame;
import com.research.camhttptest.cv.CvFrameProcess;
import com.research.camhttptest.cv.CvFrameProcess.FrameDataCallback;
import com.research.camhttptest.gl.CamData;
import com.research.camhttptest.gl.GLRenderer;
import com.research.camhttptest.http.AsyncClientRunner;
import com.research.camhttptest.http.AsyncClientRunner.RequestListener;
import com.research.camhttptest.http.Client;
import com.research.camhttptest.http.JsonParser;

public class MainActivity extends Activity implements LocationListener, RequestListener{
	private static final String TAG = "CamHttpTest";
	private static final String UPLOAD_URL = "http://172.16.146.201:8000/api/upload_file/";
	private final static String INPUT_IMG_FILENAME = "/temp.jpg"; 
	
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
	
    /**
     * Http related
     */
	private Client client;
	private AsyncClientRunner asyncrunner;
	private Button mButton;
	
	/**
	 * Image byte array
	 */
	private byte[] imageByteArray;
    
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
	
	
	
	//process the received frame data
	private FrameDataCallback mFrameDataCallback = new FrameDataCallback(){
		@Override
		public void onFrameDataReceived(CameraFrame frame, int width, int height) {
			//String filepath = Environment.getExternalStorageDirectory().toString() + INPUT_IMG_FILENAME;
	  		//FileOutputStream fileOutputStream = null;
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
	  		synchronized(this){
	  			if (frame != null){
	  				//try {  			 			
	  					Bitmap myImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565); 
	  					Utils.matToBitmap(frame.gray(), myImage);
	  			
	  					//fileOutputStream = new FileOutputStream(
	  					//		filepath);							
	 
	  					//BufferedOutputStream bos = new BufferedOutputStream(
	  					//		fileOutputStream);
	  			
	  					//compress image to jpeg
	  					myImage.compress(CompressFormat.PNG, 75, stream);
	  					imageByteArray = stream.toByteArray();
	
	  					//bos.flush();
	  					//bos.close();  			
	  					//fileOutputStream.close();  			
	  				}

	  		this.notify();
			}
		//}
			
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
		
        longView = (TextView)findViewById(R.id.longitude);
        latView = (TextView)findViewById(R.id.latitude);
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        
		client = Client.getInstance();
		asyncrunner = new AsyncClientRunner(client);
		mButton = (Button)findViewById(R.id.button);
		mButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	UploadImage();
            }
        });
	}
	
	private void UploadImage(){
		mCvFrameProcess.takePicture(mFrameDataCallback);
		
		String filepath = Environment.getExternalStorageDirectory().toString() + INPUT_IMG_FILENAME;

		String[] identifier = {"image_byte_array", "longitude", "latitude", "location_name", "object_name"};
		String[] value = {filepath, 
				"100.0", "100.0", "location1", "object1"};
		
		Bundle params = new Bundle();
		params.putByteArray(identifier[0], imageByteArray);
		params.putString(identifier[1], String.valueOf(longitude));
		params.putString(identifier[2], String.valueOf(latitude));
		params.putString(identifier[3], value[3]);
		params.putString(identifier[4], value[4]);
		
		
		asyncrunner.request(this, 
				UPLOAD_URL, 
				params, 
				Client.HTTPMETHOD_POST, 
				"file", 
				this, 
				"upload image");
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

	@Override
	public void onComplete(String response, Object state) {
		final String result = JsonParser.ClassParse(response);
		runOnUiThread(new Runnable(){
			@Override
			public void run(){
				Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
			}
		});

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


}
