package com.research.camhttptest.http;


/**
 * This class contains the api urls as constant, to be request by the client
 * @author yulu
 *
 */

public class HttpConstant {
	
	public static final String BASE_URI = "http://172.16.146.201:8000/api/";
	public static final String DOWNLOAD_IMG_URI = "http://172.16.146.201:8000/media/";

	public static final String UPLOAD_FOR_RECO_URI = BASE_URI + "upload_image_for_match/";
	public static final String REQUEST_OBJECT_IMG_URI = BASE_URI + "request_image_url";

}
