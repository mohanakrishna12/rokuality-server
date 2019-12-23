package com.rokuality.server.driver.device.roku;

import org.apache.commons.io.IOUtils;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import com.rokuality.server.enums.RokuAPIType;

public class RokuDevAPIManager {

	private static final int DEFAULT_API_TIMEOUT_MS = 10000;

	private RokuAPIType apiType = null;
	private JSONObject requestPayload = null;
	private String deviceip = null;
	private String path = null;
	private String method = null;
	private int responseCode = 400;
	private String responseContent = null;

	public RokuDevAPIManager(RokuAPIType apiType, String deviceip, String path, String method) {
		this.apiType = apiType;
		this.deviceip = deviceip;
		this.path = path;
		this.method = method;
	}

	public void addRequestPayload(JSONObject payload) {
		requestPayload = payload;
	}

	public boolean sendDevAPICommand() {
		if (deviceip == null || path == null || method == null) {
			Log.getRootLogger().warn(String.format("Null data %s, %s, %s. NOT sending request to dev api!",
					String.valueOf(deviceip), String.valueOf(path), String.valueOf(method)));
			return false;
		}

		try {
			String urlLoc = "http://" + deviceip + ":" + getAPIPort() + path;
			URL url = new URL(urlLoc);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(DEFAULT_API_TIMEOUT_MS);
			con.setReadTimeout(DEFAULT_API_TIMEOUT_MS);
			con.setRequestMethod(method.toUpperCase());

			if (requestPayload != null) {
				con.setRequestProperty("Content-Type", "application/json; utf-8");
				try (OutputStream outputStream = con.getOutputStream()) {
					byte[] input = requestPayload.toJSONString().getBytes("utf-8");
					outputStream.write(input, 0, input.length);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			responseCode = con.getResponseCode();
			if (responseCode == HttpStatus.SC_OK) {
				try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
					responseContent = IOUtils.toString(bufferedReader);
				} catch (IOException e) {
					Log.getRootLogger().warn(e);
				}
			} else if (responseCode >= HttpStatus.SC_BAD_REQUEST) {
				try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
					Log.getRootLogger().warn(IOUtils.toString(bufferedReader));
				} catch (IOException e) {
					Log.getRootLogger().warn(e);
				}
			} else {
				// UNKNOWN TODO
			}
			con.disconnect();
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		return responseCode == HttpStatus.SC_OK;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getResponseContent() {
		return responseContent;
	}

	private int getAPIPort() {
		return this.apiType.equals(RokuAPIType.WEBDRIVER) ? 9000 : 8060;
	}

}


