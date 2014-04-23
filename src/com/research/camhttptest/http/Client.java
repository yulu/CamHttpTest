package com.research.camhttptest.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.content.Context;
import android.os.Bundle;

/**
 * A singleton class to encapsulate the url request methods
 * @author yulu
 */

public class Client {
	
	public static final String HTTPMETHOD_POST = "POST";
	public static final String HTTPMETHOD_GET = "GET";
	public static final String HTTPMETHOD_DELETE = "DELETE";
	
	private static Client mClient = null;

	private Client(){

	}

	public synchronized static Client getInstance(){
		if(mClient == null){
			mClient = new Client();
		}
		return mClient;
	}

	/**
	 * Request Client api by GET or POST, return Json String
	 * 
	 * @param url Openapi request URL
	 * @param http GET or POST parameters
	 * @param httpMethod: e.g. "GET", "POST", "DELETE"
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public String request(Context context, String url, Bundle params, String httpMethod, String server_entry)
			throws FileNotFoundException, MalformedURLException, IOException{

		String rlt = Utility.openUrlForString(context, url, httpMethod, params, server_entry);

		return rlt;
	}
	
	/**
	 * Request Client api by GET or POST, return byte array (binary data)
	 * 
	 * @param url Openapi request URL
	 * @param http GET or POST parameters
	 * @param httpMethod: e.g. "GET", "POST", "DELETE"
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public byte[] requestForByteArray(Context context, String url, Bundle params, String httpMethod, String server_entry)
			throws FileNotFoundException, MalformedURLException, IOException{
		byte[] response = Utility.openUrlForByteArray(context, url, httpMethod, params, server_entry);
		
		return response;
		
	}
}