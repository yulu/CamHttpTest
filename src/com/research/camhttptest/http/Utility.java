package com.research.camhttptest.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;


/**
 * Utility class for http request
 * @author yulu
 */

public class Utility {
	public static final String IMAGE_FILENAME = "file";
	public static final String DATA_FILENAME = "upload_data";
	public static final String IMAGE_BYTE_ARRAY = "image_byte_array";
	
	public static final String BOUNDARY = "-------";
	public static final String MP_BOUNDARY = "--" + BOUNDARY;
	public static final String END_MP_BOUNDARY = "--" + BOUNDARY + "--";
	
	public static final String MULTIPART_FORM_DATA = "multipart/form-data";
	
	private static final int SET_CONNECTION_TIMEOUT = 5000;
	private static final int SET_SOCKET_TIMEOUT = 5000;
	
	/**
	 * Write POST content to a form (as String)
	 * @param parameters
	 * @param boundary
	 * @return
	 */
	public static String encodePostBody(Bundle parameters, String boundary){
		if(parameters == null)
			return "";
		StringBuilder sb = new StringBuilder();
		
		for(String key : parameters.keySet()){
			if(parameters.getByteArray(key) != null){
				continue;
			}
			
			sb.append("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n"
					+ parameters.getString(key));
			sb.append("\r\n" + "--" + boundary + "\r\n");
			
		}
		return sb.toString();
	}
	
	
	public static String encodeUrl(Bundle parameters){
		if(parameters == null){
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String key : parameters.keySet()) {
            Object parameter = parameters.get(key);
            if (!(parameter instanceof String)) {
                continue;
            }

            if (first) 
            	first = false; 
            else 
            	sb.append("&");
            
            sb.append(URLEncoder.encode(key) + "=" +
                      URLEncoder.encode(parameters.getString(key)));
        }
        return sb.toString();
	}
	
	public static Bundle decodeUrl(String s){
		Bundle params = new Bundle();
		if(s != null){
			String array[] = s.split("&");
			for(String parameter : array){
				String v[] = parameter.split("=");
				params.putString(URLDecoder.decode(v[0]), URLDecoder.decode(v[1]));
			}
		}
		return params;
	}
	
	/**
	 * Get content from a form (as FormEntity)
	 * @param params
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static UrlEncodedFormEntity getPostParamters(Bundle params)
		throws UnsupportedEncodingException{
		if(params == null || params.isEmpty()){
			return null;
		}
		try{
			int size = params.size();
			List<NameValuePair> form = new ArrayList<NameValuePair>(size);
			
			for(int i = 0; i < size; i++){
				Object[] keys = params.keySet().toArray();
				form.add(new BasicNameValuePair((String) keys[i], params.getString((String) keys[i])));
			}
			
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, "UTF-8");
			return entity;
			
		}catch(UnsupportedEncodingException e){
			throw new UnsupportedEncodingException();
		}
	}
	
	public static String encodeParameters(Bundle params){
		if(null == params || Utility.isBundleEmpty(params)){
			return "";
		}
		
		StringBuilder buf = new StringBuilder();
		int j = 0 ;
		for (String key : params.keySet()) {
            Object parameter = params.get(key);
            if (!(parameter instanceof String)) {
                continue;
            }else{
            	if(j != 0 ){
            		buf.append("&");
            	}try{
            		buf.append(URLEncoder.encode(key, "UTF-8")).append("=").append(
            				URLEncoder.encode(params.getString(key), "UTF-8"));
            	}catch(java.io.UnsupportedEncodingException neverHappen){
            		
            	}
            	j++;
            }
		}
        return buf.toString();
	}
	
	public static boolean isBundleEmpty(Bundle bundle){
		if(bundle == null || bundle.size() == 0){
			return true;
		}
		return false;
	}
	
	/**
	 * HTTP request: return json as a string
	 * 
	 * @param context: context of activity
	 * @param url: request url of open api
	 * @param method: HTTP METHOD, GET, POST, DELETE
	 * @param params: http params, query or postparameters
	 */	
	public static String openUrlForString(	Context context, 
											String url, 
											String method, 
											Bundle params, 
											String server_entry) 
	
			throws MalformedURLException, IOException {
		HttpResponse response = null;
		HttpClient httpClient = new DefaultHttpClient();
		httpClient = sslClient(httpClient);		
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), SET_CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), SET_SOCKET_TIMEOUT);
		
		String result="";
		String file = "";
		byte[] payload = null;
		
		for (String key : params.keySet()) {
            	if(key.equals(IMAGE_FILENAME)){
            		file = params.getString(key);
            		break;
            	}
            	if(key.equals(DATA_FILENAME)){
            		payload = params.getByteArray(key);
            		break;         	
            	}
            	if(key.equals(IMAGE_BYTE_ARRAY)){
            		payload = params.getByteArray(key);
            		break;
            	}
		}
		
		try{		
			HttpUriRequest request = null;
			ByteArrayOutputStream bos = null;
			
			//GET request
			if(method.equals("GET")){
				url = url + "?" + encodeUrl(params);
				HttpGet get = new HttpGet(url);
				request = get;			
			
			//POST request
			}else if(method.equals("POST")){
				HttpPost post = new HttpPost(url);
				byte[] data = null;
				bos = new ByteArrayOutputStream(1024*50);			
				
				//upload bitmap file
				if(!TextUtils.isEmpty(file)){
					Utility.paramToUpload(bos, params);
					post.setHeader("Content-Type", MULTIPART_FORM_DATA + "; boundary = " + BOUNDARY);
					Bitmap bf = BitmapFactory.decodeFile(file);
					Utility.imageToUpload(bos, bf, server_entry);
					
				//upload normal file
				}else if(payload!=null){
					Utility.paramToUpload(bos, params);
					post.setHeader("Content-Type", MULTIPART_FORM_DATA + "; boundary = " + BOUNDARY);
					Utility.imageByteToUpload(bos, payload, server_entry);
				}
				else{
					post.setHeader("Content-Type", "application/x-www-form-urlencoded");
					String postParam = encodeParameters(params);
					data = postParam.getBytes("UTF-8");
					bos.write(data);
				}
				
				data = bos.toByteArray();
				bos.close();
				ByteArrayEntity formEntity = new ByteArrayEntity(data);
				post.setEntity(formEntity);
				request = post;
				
			//DELETE request
			}else if(method.equals("DELETE")){
				request = new HttpDelete(url);
			}
			
			//set header, maybe more to add
			request.setHeader("User-Agent", System.getProperties().getProperty("http.agent"));
			
			//execute the request here~~~~~
			response = httpClient.execute(request);
					
			StatusLine status = response.getStatusLine();
			int statusCode = status.getStatusCode();
		
			if(statusCode == 200 || statusCode == 400){
				
				result = read(response);
				return result;
				
			}else{
				Log.i("http", "Download failed, HTTP response code "
	                    + statusCode + " - " + status.getReasonPhrase());
	            return "Download failed, HTTP response code "
	                    + statusCode + " - " + status.getReasonPhrase();
			}

		
		}catch(IOException e){
			 if(response != null){
				 System.err.println(readError(response.getAllHeaders()));
				 return readError(response.getAllHeaders()); 
			 }else{
				 return e.toString();
			 }
		}
		
	}
	
	/**
	 * HTTP request: return byte array
	 * 
	 * @param context: context of activity
	 * @param url: request url of open api
	 * @param method: HTTP METHOD, GET, POST, DELETE
	 * @param params: http params, query or postparameters
	 */	
	public static byte[] openUrlForByteArray(	Context context, 
												String url, 
												String method, 
												Bundle params, 
												String server_entry) 
	
			throws MalformedURLException, IOException {
		HttpResponse response;
		HttpClient httpClient = new DefaultHttpClient();
		httpClient = sslClient(httpClient);		
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), SET_CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), SET_SOCKET_TIMEOUT);
		
		String result="";
		String file ="";
		byte[] payload = null;
		
		for (String key : params.keySet()) {
            	if(key.equals(IMAGE_FILENAME)){
            		file = params.getString(key);
            		break;
            	}
            	if(key.equals(DATA_FILENAME)){
            		payload = params.getByteArray(key);
            		break;         	
            }
		}
		
		try{		
			HttpUriRequest request = null;
			ByteArrayOutputStream bos = null;
			
			//GET request
			if(method.equals("GET")){
				url = url + "?" + encodeUrl(params);
				HttpGet get = new HttpGet(url);
				request = get;			
			
			//POST request
			}else if(method.equals("POST")){
				HttpPost post = new HttpPost(url);
				byte[] data = null;
				bos = new ByteArrayOutputStream(1024*50);			
				
				//upload bitmap file
				if(!TextUtils.isEmpty(file)){
					Utility.paramToUpload(bos, params);
					post.setHeader("Content-Type", MULTIPART_FORM_DATA + "; boundary = " + BOUNDARY);
					Bitmap bf = BitmapFactory.decodeFile(file);
					Utility.imageToUpload(bos, bf, server_entry);
					
				//upload normal file
				}else if(payload!=null){
					Utility.paramToUpload(bos, params);
					post.setHeader("Content-Type", MULTIPART_FORM_DATA + "; boundary = " + BOUNDARY);
					Utility.fileToUpload(bos, payload, server_entry);
				}
				else{
					post.setHeader("Content-Type", "application/x-www-form-urlencoded");
					String postParam = encodeParameters(params);
					data = postParam.getBytes("UTF-8");
					bos.write(data);
				}
				
				data = bos.toByteArray();
				bos.close();
				ByteArrayEntity formEntity = new ByteArrayEntity(data);
				post.setEntity(formEntity);
				request = post;
				
			//DELETE request
			}else if(method.equals("DELETE")){
				request = new HttpDelete(url);
			}
			
			//set header, maybe more to add
			request.setHeader("User-Agent", System.getProperties().getProperty("http.agent"));
			
			//execute the request here~~~~~
			response = httpClient.execute(request);		
		
			StatusLine status = response.getStatusLine();
			int statusCode = status.getStatusCode();
		
			if(statusCode != 200){
				result = readError(response.getAllHeaders());
			
				 Log.i("http", "Error to download binary, HTTP response code "
		                    + statusCode + " - " + status.getReasonPhrase() + ". Response: "+result);
			
				return null;
			}
		
			HttpEntity entity = response.getEntity();
			byte[] bytes = EntityUtils.toByteArray(entity);
			return bytes;
		
			}catch(IOException e){
				Log.i("http", "Open Binary exception: "+ e.toString());
				return null;
		}
	}
	
	private static String readError(Header[] headers) {
		
		if(headers != null && headers.length > 0){
			return headers[0].getValue();
		}
		return null;
	}
	
	private static void fileToUpload(OutputStream out, byte[] payload, String server_entry){
		StringBuilder temp = new StringBuilder();
		final String serverFileName = "file" +".dat"; //amend later
		
		temp.append(MP_BOUNDARY).append("\r\n");
		temp.append("Content-Disposition: form-data; name=\"").append(
				server_entry).append("\"; filename=\"").append(
				serverFileName).append("\"\r\n");
		String filetype = "application/octet-stream";
		temp.append("Content-Type: ").append(filetype).append("\r\n\r\n");
		
		byte[] res = temp.toString().getBytes();
		BufferedInputStream bis = null;
		
		try{
			out.write(res);
			out.write(payload, 0, payload.length);
			out.write("\r\n".getBytes());
			out.write(("\r\n" + END_MP_BOUNDARY).getBytes());
		}catch(IOException e){
			//throw exception
		}finally{
			if(null != bis){
				try{
					bis.close();
				}catch(IOException e){
					//throw exception
				}
			}
		}
	}
	
	private static void imageToUpload(OutputStream out, Bitmap imgpath, String server_entry){
		StringBuilder temp = new StringBuilder();
		final String serverFileName = "test"+ (int) Math.round(Math.random()*1000) + ".jpg";	
		
		temp.append(MP_BOUNDARY).append("\r\n");
		temp.append("Content-Disposition: form-data; name=\"").append(
				server_entry).append("\"; filename=\"").append(
				serverFileName).append("\"\r\n");
		String filetype = "image/jpg";
		temp.append("Content-Type: ").append(filetype).append("\r\n\r\n");
		
		byte[] res = temp.toString().getBytes();
		BufferedInputStream bis = null;
		
		try{
			out.write(res);
			imgpath.compress(CompressFormat.PNG, 75, out);
			out.write("\r\n".getBytes());
			out.write(("\r\n" + END_MP_BOUNDARY).getBytes());
		}catch(IOException e){
			System.err.println(e);
		}finally{
			if(null != bis){
				try{
					bis.close();
				}catch(IOException e){
					//throw exception
				}
			}
		}
	}
	
	//test to upload image from byte[]
	private static void imageByteToUpload(OutputStream out, byte[] imgByteArray, String server_entry){
		StringBuilder temp = new StringBuilder();
		final String serverFileName = "test"+ (int)Math.round(Math.random()*1000) + ".jpg";
		
		temp.append(MP_BOUNDARY).append("\r\n");
		temp.append("Content-Disposition: form-data; name=\"").append(
				server_entry).append("\"; filename=\"").append(
				serverFileName).append("\"\r\n");
		String filetype = "image/jpg";
		temp.append("Content-Type: ").append(filetype).append("\r\n\r\n");
		
		byte[] res = temp.toString().getBytes();
		
		try{
			out.write(res);
			out.write(imgByteArray);
			out.write("\r\n".getBytes());
			out.write(("\r\n" + END_MP_BOUNDARY).getBytes());
		}catch(IOException e){
			System.err.println(e);
		}
	}
	
	private static void paramToUpload(OutputStream baos, Bundle params){
		
		for (String key : params.keySet()) {
            Object parameter = params.get(key);
            if (!(parameter instanceof String)) {
                continue;
            }else{
            	StringBuilder temp = new StringBuilder(10);
            	temp.setLength(0);
            	temp.append(MP_BOUNDARY).append("\r\n");
            	temp.append("Content-Disposition: form-data; name=\"").append(key).append(
            			"\"\r\n\r\n");
            	temp.append(params.getString(key)).append("\r\n");
            	byte[] res = temp.toString().getBytes();
            	try{
            		baos.write(res);
            	}catch(IOException e){
            		//throw exceptions
            	}       	         	
            }
		}
	}
	
	/**
	 * Read http requests result from response
	 * @param response
	 * @return String
	 */
	private static String read(HttpResponse response)
		throws IllegalStateException, IOException{
		String result = "";
		HttpEntity entity = response.getEntity();
		InputStream inputStream;
		
		try{
			inputStream = entity.getContent();
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			
			Header header = response.getFirstHeader("Content-Encoding");
			if(header != null && header.getValue().toLowerCase().indexOf("gzip") > 1){
				inputStream = new GZIPInputStream(inputStream);
			}
			
			//Read response into a buffered stream
			int readBytes = 0;
			byte[] sBuffer = new byte[512];
			while((readBytes = inputStream.read(sBuffer)) != -1){
				content.write(sBuffer, 0, readBytes);
			}
			
			//Return result from buffered stream
			result = new String(content.toByteArray());
			//result = read(inputStream);
			return result;
			
		}catch(IllegalStateException e){
			throw new IllegalStateException();
		}catch(IOException e){
			throw new IOException();
		}
	}
	
	/**
	 * get the http client object
	 */
	private static HttpClient sslClient(HttpClient client) {
	    try {
	        X509TrustManager tm = new X509TrustManager() { 
	        	
	        	
	            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
	            }

	            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
	            }

	            public X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	        };
	        
	        X509HostnameVerifier verifier = new X509HostnameVerifier() {
	        	 
                @Override
                public void verify(String hostname, SSLSocket ssls) throws IOException {
                }
 
                @Override
                public void verify(String hostname, X509Certificate xc) throws SSLException {
                }
 
                @Override
                public void verify(String hostname, String[] strings, String[] strings1) throws SSLException {
                }
 
                @Override
                public boolean verify(String hostname, SSLSession ssls) {
                    return false;
                }
	        	        	
            };
            
            SSLContext ctx = SSLContext.getInstance("TLS");
	        ctx.init(null, new TrustManager[]{tm}, null);
	        SSLSocketFactory ssf = new MySSLSocketFactory(ctx);
	        ssf.setHostnameVerifier(verifier);
	        ClientConnectionManager ccm = client.getConnectionManager();
	        SchemeRegistry sr = ccm.getSchemeRegistry();
	        sr.register(new Scheme("https", ssf, 443));
	        return new DefaultHttpClient(ccm, client.getParams());
	    } catch (Exception ex) {
	        return null;
	    }
	}
	
	/**
	 * Socket Factory
	 * @author yulu
	 *
	 */	
	public static class MySSLSocketFactory extends SSLSocketFactory{
		SSLContext sslContext = SSLContext.getInstance("TLS");
		
		public MySSLSocketFactory(KeyStore truststore) 
				throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException{
			super(truststore);
			
			TrustManager tm = new X509TrustManager(){
				public void checkClientTrusted(X509Certificate[] chain, String authType)
					throws CertificateException{
					
				}
				
				public void checkServerTrusted(X509Certificate[] chain, String authType)
					throws CertificateException{
					
				}
				
				public X509Certificate[] getAcceptedIssuers(){
					return null;
				}
			};
			
			sslContext.init(null, new TrustManager[] {tm}, null);
		}
		
		public MySSLSocketFactory(SSLContext context)
				throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
			super(null);
			sslContext = context;
		}
		
		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
			throws IOException, UnknownHostException{
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}
		
		@Override
		public Socket createSocket() throws IOException{
			return sslContext.getSocketFactory().createSocket();
		}
	}
	
}


