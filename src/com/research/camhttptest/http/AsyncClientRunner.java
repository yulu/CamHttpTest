package com.research.camhttptest.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.content.Context;
import android.os.Bundle;

/**
 * Implement an Http API as an asynchronized way. Every object uses this runner 
 * should implement interface RequestListener
 * @author yulu
 */
public class AsyncClientRunner {
	private Client mClient;

	public AsyncClientRunner(Client Client){
		this.mClient = Client;
	}

	public void request(final Context context,
						final String url,
						final Bundle params,
						final String httpMethod,
						final String server_entry,
						final RequestListener listener,
						final Object state){
		new Thread(){
			@Override
			public void run(){
				try{
					String resp = mClient.request(context, url, params, httpMethod, server_entry);
					listener.onComplete(resp, state);
				}catch(FileNotFoundException e){
					listener.onFileNotFoundException(e, state);
				}catch(MalformedURLException e){
					listener.onMalformedURLException(e, state);
				}catch(IOException e){
					listener.onIOException(e, state);
				}
			}
		}.start();
	}

	public void requestForByteArray(final Context context,
			final String url,
			final Bundle params,
			final String httpMethod,
			final String server_entry,
			final RequestListener listener,
			final Object state){
		new Thread(){
			@Override
			public void run(){
				try{
					byte[] resp = mClient.requestForByteArray(context, url, params, httpMethod, server_entry);
					listener.onComplete(resp, state);
				}catch(FileNotFoundException e){
					listener.onFileNotFoundException(e, state);
				}catch(MalformedURLException e){
					listener.onMalformedURLException(e, state);
				}catch(IOException e){
					listener.onIOException(e, state);
				}
			}
		}.start();
	}
	

	public static interface RequestListener{
		public void onComplete(String response, Object state);
    	public void onComplete(byte[] response, Object state);
		public void onIOException(IOException e, Object state);
		public void onFileNotFoundException(FileNotFoundException e, Object state);
		public void onMalformedURLException(MalformedURLException e, Object state);
	}
}
