/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.example.hello;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.apache.cordova.CordovaActivity;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

public class CordovaApp extends CordovaActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.init();
        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
        
       // new downloadAppTask().execute("http://172.31.152.26/apk/agent.apk");
       // new downloadFromCatalystTask().execute();
    }
    
    class downloadAppTask extends AsyncTask<String, Void, Boolean>{
    	File apkFile = null;
    	FileOutputStream apkStream = null;
    	@Override
    	protected void onPreExecute() {
    		try {
    			File downloadFile = new File("/data/data/" + getPackageName() + "/files");
    			if (!downloadFile.exists()) {
    				downloadFile.mkdirs();
    			}
    			downloadFile.setExecutable(true, false);
    			cleanOldFiles("temp", ".apk", downloadFile);
    			apkFile = File.createTempFile("temp", ".apk", downloadFile);
    			apkFile.deleteOnExit();
    			apkStream = new FileOutputStream(apkFile);
    		}catch(Exception e){
    			e.printStackTrace();
    		}
    	}
    	@Override
    	protected Boolean doInBackground(String... params) {
    		return downloadNukonaApplication("http://172.31.152.26/apk/agent.apk", apkStream);
    	}
    	@Override
    	protected void onPostExecute(Boolean result) {
    		if(result){
    			installPackage(Uri.fromFile(apkFile));
    		}
    	}
    }
    private boolean downloadAndInstallAgent(String location){
		boolean status = false;
		FileOutputStream apkStream = null;
		try {
			File downloadFile = new File("/data/data/" + getPackageName() + "/files");
			if (!downloadFile.exists()) {
				downloadFile.mkdirs();
			}
			downloadFile.setExecutable(true, false);
			cleanOldFiles("temp", ".apk", downloadFile);
			File apkFile = File.createTempFile("temp", ".apk", downloadFile);
			apkFile.deleteOnExit();
			apkStream = new FileOutputStream(apkFile);
			boolean result = downloadNukonaApplication(location, apkStream);
			if(result){
				installPackage(Uri.fromFile(apkFile));
				status = true;
			}
		} catch (IOException e) {	
			
			e.printStackTrace();
		} finally {
			if (apkStream != null)
				try {
					apkStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return status;
	}
    
    private boolean downloadNukonaApplication(String url, FileOutputStream apkStream){
    	boolean status = false;
    	DefaultHttpClient client = new DefaultHttpClient(getTimeoutParameters());

        // Prepare a request object
        HttpGet request = new HttpGet(url);

        String lang = getBaseContext().getResources().getConfiguration().locale.toString();
        if (lang.length() > 0) {
            lang = lang.toLowerCase().replace("_", "-");
            request.setHeader("Accept-Language", lang);
        }

        try {
            client.setCookieStore((CookieStore) getLocalContext().getAttribute(
                ClientContext.COOKIE_STORE));

            HttpResponse response = client.execute(request, getLocalContext());
            StatusLine httpstatus = response.getStatusLine(); // Check if server
                                                          // response is valid
            if (httpstatus.getStatusCode() != 200) {
                return status;
            }

            status = pullContentFromResponse(response, apkStream);
        } catch (Exception e) {
        	
        }
        return status;
    }
    
    private boolean pullContentFromResponse(HttpResponse response, FileOutputStream outputStream){
    	boolean status = false;
    	byte[] sBuffer = new byte[512];
        // Pull content stream from response
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();

                    // Read response into a buffered stream
                    int readBytes = 0;
                    while ((readBytes = inputStream.read(sBuffer)) != -1) {
                        outputStream.write(sBuffer, 0, readBytes);
                    }
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
                status = true;
            }
        } catch (Exception e) {
            ;
        }
    	return status;
    }
    
    private void installPackage(Uri fileLocation){
		try{
			Intent intent = new Intent(Intent.ACTION_VIEW);			
			intent.setDataAndType(fileLocation, "application/vnd.android.package-archive");
			intent.putExtra("android.intent.extra.RETURN_RESULT", true);
			intent.putExtra("android.intent.extra.NOT_UNKNOWN_SOURCE", true);
			intent.putExtra("android.intent.extra.EXTRA_INSTALLER_PACKAGE_NAME", getPackageName());
			startActivityForResult(intent, 100);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
    
    private void downloadAndInstallApp(String apkurl){
		try {

			File downloadFile = new File("/data/data/" + getPackageName() + "/files");
			File apkFile =  null;
			if (!downloadFile.exists()) {
				downloadFile.mkdirs();
			}
			downloadFile.setExecutable(true, false);
			cleanOldFiles("tmp", ".apk", downloadFile);
			apkFile = File.createTempFile("tmp", ".apk", downloadFile);
			apkFile.deleteOnExit(); // android program's done exit!


			FileOutputStream apkStream = new FileOutputStream(apkFile);

			DefaultHttpClient client = new DefaultHttpClient(getTimeoutParameters());

			// Prepare a request object
			HttpGet request = new HttpGet(apkurl);

			String lang = getBaseContext().getResources().getConfiguration().locale.toString();
			if (lang.length() > 0) {
				lang = lang.toLowerCase().replace("_", "-");
				request.setHeader("Accept-Language", lang);
			}

			client.setCookieStore((CookieStore) getLocalContext().getAttribute(
					ClientContext.COOKIE_STORE));

			HttpResponse response = client.execute(request, this.getLocalContext());
			StatusLine status = response.getStatusLine(); // Check if server
			// response is valid
			if (status.getStatusCode() != 200) {
				System.out.println("** Status code : " + status.getStatusCode());
				return;
			}
	        
			byte[] sBuffer = new byte[1024];
			// Pull content stream from response
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = null;
				try {
					inputStream = entity.getContent();

					// Read response into a buffered stream
					int readBytes = 0;
					while ((readBytes = inputStream.read(sBuffer)) != -1) {
						apkStream.write(sBuffer, 0, readBytes);
					}
					apkStream.close();
				} finally {
					if (inputStream != null) {
						inputStream.close();
					}
					entity.consumeContent();
				}
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
				startActivity(intent);
				
			}
		} catch (Exception e) {
			System.out.println("*** " + e.getMessage());
		}
	}  
	/**
	 * Clean old files
	 * @param prefix
	 * @param suffix
	 * @param directory
	 */
	private void cleanOldFiles(String prefix, String suffix, File directory)
	{
		final int DAYS = (24 * 60 * 60 * 1000);
		String[] filenames = directory.list();

		for (String filename : filenames) {
			if (filename.startsWith(prefix) && filename.endsWith(suffix)) {
				File f = new File(directory, filename);
				long a = new Date().getTime() - f.lastModified();
				if (a > (5*DAYS)) {
					f.delete();
				}
			}
		}
	}
	/**
	 * Method to get http context
	 * @return
	 */
	private  HttpContext getLocalContext() {
		HttpContext localContext = new BasicHttpContext();
		// Create a local instance of cookie store
		CookieStore newCookieStore = new BasicCookieStore();
		localContext.setAttribute(ClientContext.COOKIE_STORE, newCookieStore);

		return localContext;
	} 
	/**
	 * Method to get timeout parameters
	 * @return
	 */
	private   HttpParams getTimeoutParameters() {
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		int timeoutConnection = 25000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 25000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		// Return parameters
		return httpParameters;
	}
	
	
	/////////////// Catalyst code
	
	class downloadFromCatalystTask extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... params) {
			try {
				downloadFile("http://172.31.152.26/apk/agent.apk", "newapp.apk");
				installPackage("newapp.apk");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}
	private void downloadFile(String source, String target) throws IOException
	{
		HttpURLConnection uc = (HttpURLConnection)new URL(source).openConnection();
		try {
			uc.setAllowUserInteraction(false);
			uc.setConnectTimeout(10000);
			uc.setReadTimeout(10000);
			uc.setUseCaches(false);
			int rc = uc.getResponseCode();
			if (rc != HttpURLConnection.HTTP_OK) {
				throw new IOException(rc + " " + uc.getResponseMessage());
			}
			
			InputStream is = uc.getInputStream();
			try {
				OutputStream os = openFileOutput(target, MODE_WORLD_READABLE);
				try {
					byte[] buffer = new byte[8192];
					while (true) {
						
						int count = is.read(buffer);
						if (count <= 0) {
							break;
						}
						os.write(buffer, 0, count);
					}
				}
				finally {
					try {
						os.close();
					}
					catch (Exception e) {
					}
				}
			}
			finally {
				try {
					is.close();
				}
				catch (Exception e) {
				}
			}
		}
		finally {
			try {
				uc.disconnect();
			}
			catch (Exception e) {
			}
		}
	}

	private void installPackage(String filename)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(getFileStreamPath(filename)), "application/vnd.android.package-archive");
		intent.putExtra("android.intent.extra.RETURN_RESULT", true);
		intent.putExtra("android.intent.extra.NOT_UNKNOWN_SOURCE", true);
		intent.putExtra("android.intent.extra.EXTRA_INSTALLER_PACKAGE_NAME", getPackageName());
		startActivity(intent);
	}

}
