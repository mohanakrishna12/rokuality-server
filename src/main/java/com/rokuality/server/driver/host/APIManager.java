package com.rokuality.server.driver.host;

import org.apache.commons.io.IOUtils;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import com.rokuality.server.driver.device.xbox.XBoxSmartglassFactory;
import com.rokuality.server.enums.APIType;

public class APIManager {

	private static final int DEFAULT_API_TIMEOUT_MS = 10000;

	private APIType apiType = null;
	private JSONObject requestPayload = null;
	private String deviceip = null;
	private String path = null;
	private String method = null;
	private int responseCode = 400;
	private String responseContent = null;

	public APIManager(APIType apiType, String deviceip, String path, String method) {
		this.apiType = apiType;
		this.deviceip = deviceip;
		this.path = path;
		this.method = method;
	}

	public void addRequestPayload(JSONObject payload) {
		this.requestPayload = payload;
	}

	public boolean sendDevAPICommand() {
		if ((deviceip == null && !apiType.equals(APIType.XBOX_SMARTGLASS)) || path == null || method == null) {
			Log.getRootLogger().warn(String.format("Null data %s, %s, %s. NOT sending request to dev api!",
					String.valueOf(deviceip), String.valueOf(path), String.valueOf(method)));
			return false;
		}

		try {
			String urlLoc = "http://" + getHost() + ":" + getAPIPort() + path;
			URL url = new URL(urlLoc);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(DEFAULT_API_TIMEOUT_MS);
			con.setReadTimeout(DEFAULT_API_TIMEOUT_MS);
			con.setRequestMethod(method.toUpperCase());

			if (requestPayload != null) {
				con.setDoOutput(true);
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
					responseContent = IOUtils.toString(bufferedReader);
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

		return HttpStatus.SC_OK == responseCode;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getResponseContent() {
		return responseContent;
	}

	private int getAPIPort() {
		switch (this.apiType) {
			case ROKU_WEBDRIVER:
				return 9000;
			case ROKU_DEV_API:
				return 8060;
			case XBOX_SMARTGLASS:
				return XBoxSmartglassFactory.PORT;
			default:
				return 9000;
		}
	}

	private String getHost() {
		switch (this.apiType) {
			case ROKU_WEBDRIVER:
				return "localhost";
			case ROKU_DEV_API:
				return deviceip;
			case XBOX_SMARTGLASS:
				return "localhost";
			default:
				return "localhost";
		}
	}

}


