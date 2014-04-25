package com.research.camhttptest.http;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonParser {
	public static final String NETWORK_ERROR = "NETWORK_ERROR";
	public static final String INVALID_REQUEST = "INVALID_REQUEST";
	public static final String COMPLETE = "COMPLETE";
	
	public static String StatusParser(String response){
		try{
			if(response != null){
				JSONObject ja = new JSONObject(response);
				String status = ja.getString("status");
				if(status.equals("success"))
					return COMPLETE;
				else
					return INVALID_REQUEST;
			}else
				return NETWORK_ERROR;
		}catch(JSONException e){
			e.printStackTrace();
			return NETWORK_ERROR;
		}
	}
	
	public static String ClassParser(String response){
		try{
			if(response != null){
				JSONObject ja = new JSONObject(response);
				String class_name = ja.getString("class_name");
				return class_name;
			}else
				return NETWORK_ERROR;
		}catch(JSONException e){
			e.printStackTrace();
			return NETWORK_ERROR;
		}
	}

}
