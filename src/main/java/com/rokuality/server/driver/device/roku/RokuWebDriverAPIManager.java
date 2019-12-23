package com.rokuality.server.driver.device.roku;

import com.rokuality.server.enums.RokuAPIType;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings({ "unchecked" })
public class RokuWebDriverAPIManager {

	private String deviceIP = "";
	private int responseCode = -1;
	private JSONObject responseObj = new JSONObject();

	public RokuWebDriverAPIManager(String deviceIP) {
		this.deviceIP = deviceIP;
	}

	public String startSession() {
		JSONObject requestObj = new JSONObject();
		requestObj.put("ip", deviceIP);
		sendCommand("POST", "/v1/session", requestObj);
		return "";
	}

	public void sendCommand(String method, String path, JSONObject payload) {
		RokuDevAPIManager rokuDevAPIManager = new RokuDevAPIManager(RokuAPIType.WEBDRIVER, deviceIP, path, method);
		responseCode = rokuDevAPIManager.getResponseCode();
		try {
			responseObj = (JSONObject) new JSONParser().parse(rokuDevAPIManager.getResponseContent());
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}
	}

	public int getResponseCode() {
		return responseCode;
	}



}
