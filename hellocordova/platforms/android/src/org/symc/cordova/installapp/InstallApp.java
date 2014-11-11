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
package org.symc.cordova.installapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
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
import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;

/**
 * This class listens to the compass sensor and stores the latest heading value.
 */
public class InstallApp extends CordovaPlugin {

	private CallbackContext callbackContext;
	
	public InstallApp(CordovaInterface cordova){
		super.cordova = cordova;
	}
	
	class downloadAndInstallTask extends AsyncTask<String, Void, Void>{
		ProgressDialog progressDialog =  null;
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(cordova.getActivity());
			progressDialog.setMessage("Please wait...");
			progressDialog.show();
		}
		@Override
		protected Void doInBackground(String... params) {
			try {
				String location = params[0];
				downloadFile(location, "newapp.apk");
				installPackage("newapp.apk");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			if(progressDialog != null)
				progressDialog.dismiss();
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
				OutputStream os = cordova.getActivity().openFileOutput(target, cordova.getActivity().MODE_WORLD_READABLE);
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
		intent.setDataAndType(Uri.fromFile(cordova.getActivity().getFileStreamPath(filename)), "application/vnd.android.package-archive");
		intent.putExtra("android.intent.extra.RETURN_RESULT", true);
		intent.putExtra("android.intent.extra.NOT_UNKNOWN_SOURCE", true);
		intent.putExtra("android.intent.extra.EXTRA_INSTALLER_PACKAGE_NAME", cordova.getActivity().getPackageName());
		cordova.getActivity().startActivity(intent);
	}
	/**
	 * Method to download and install application
	 * NOTE: This is AppCenter way, which is not working
	 * @param apkurl
	 */
	private void downloadAndInstallApp(String apkurl){
		try {

			File downloadFile = new File("/data/data/" + cordova.getActivity().getPackageName() + "/files");
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

			String lang = cordova.getActivity().getBaseContext().getResources().getConfiguration().locale.toString();
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
				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Status not OK " + status.getStatusCode()));
				return;
			}

			byte[] sBuffer = new byte[512];
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
				} finally {
					if (inputStream != null) {
						inputStream.close();
					}
					entity.consumeContent();
				}
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
				cordova.getActivity().startActivity(intent);
				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "Starts installtion"));
			}
		} catch (Exception e) {
			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getLocalizedMessage()));
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
	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		this.callbackContext =  callbackContext;
		if (action.equals("download")) {
			//Get JSONObject with App name, installed_url, uninstall_url values
			String appName = args.getString(1);
			String version = args.getString(2);
			String version_Code = args.getString(3);
			String installUrl = args.getString(4);
			String appType = args.getString(0);
//			JSONObject object = args.getJSONObject(0);
//			String appName = object.getString("package_name");
//			String version = object.getString("version_string");
//			String version_Code = object.getString("version_code");
//			String installUrl = object.getString("item_url");
//			String appType = object.getString("app_type");
			System.out.println("** Object is having : "+appName+"\n"+version+"\n"+version_Code+"\n"+installUrl+"\n"+appType);
			if(appType.compareTo("native") == 0){
				//this.downloadAndInstallApp(installUrl);
				new downloadAndInstallTask().execute(installUrl);
			}else if(appType.compareTo("storeptr") == 0){
				//TODO::
			}else if(appType.compareTo("webclip") == 0){
				//TODO::
			}
		}else if(action.equals("open")){
			this.openApp(args.getJSONObject(0).getString("package_name"));
		}
		return true;
	}
	private void openApp(String packageName){
		PackageManager pm = cordova.getActivity().getPackageManager();
		Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
		if (null == launchIntent) {
			AlertDialog.Builder alert = new AlertDialog.Builder(cordova.getActivity());
			alert.setTitle("Error");
			alert.setMessage("App can not be open");
			alert.setCancelable(false);
			alert.setIcon(android.R.drawable.ic_dialog_alert);
			alert.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id)
				{
					dialog.cancel();
				}
			});
			alert.show();
		} else {
			cordova.getActivity().startActivity(launchIntent);
		}
	}
}
