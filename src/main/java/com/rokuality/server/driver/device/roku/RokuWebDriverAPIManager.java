package com.rokuality.server.driver.device.roku;

import com.rokuality.server.driver.host.APIManager;
import com.rokuality.server.enums.APIType;
import com.rokuality.server.enums.RokuWebDriverLocatorType;
import com.rokuality.server.servlets.info;
import com.rokuality.server.utils.CacheUtils;
import com.rokuality.server.utils.FileToStringUtils;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings({ "unchecked" })
public class RokuWebDriverAPIManager {

	private String deviceIP = "";
	private String sessionID = "";
	private int responseCode = -1;
	private JSONObject responseObj = new JSONObject();

	public RokuWebDriverAPIManager(String deviceIP) {
		this.deviceIP = deviceIP;
		this.sessionID = getDeviceSessionID();
	}

	public void startSession() {
		stopSession();

		JSONObject requestObj = new JSONObject();
		requestObj.put("ip", deviceIP);
		sendCommand("POST", "/v1/session", requestObj);
	}

	public void stopSession() {
		sendCommand("DELETE", "/v1/session/" + sessionID, null);
	}

	public void findElements(RokuWebDriverLocatorType by, String attribute, String value) {
		JSONObject requestObj = new JSONObject();
		requestObj.put("using", by.value());
		if (by.equals(RokuWebDriverLocatorType.ATTR)) {
			requestObj.put("attribute", attribute);
		}
		requestObj.put("value", value);

		JSONArray requestArr = new JSONArray();
		requestArr.add(requestObj);

		JSONObject elementDataObj = new JSONObject();
		elementDataObj.put("elementData", requestArr);
		
		// TODO - parent data
		sendCommand("POST", "/v1/session/" + sessionID + "/elements", elementDataObj);
	}

	public String getSource() {
		sendCommand("GET", "/v1/session/" + sessionID + "/source", null);
		if (getWebDriverResponseCode() == 0) {
			String source = (String) getResponseObj().get("value");
			return new FileToStringUtils().convertFromBaseStringToString(source);
		}
		return null;
	}

	public void getActiveElement() {
		sendCommand("POST", "/v1/session/" + sessionID + "/element/active", null);
	}

	public void getMediaPlayerInfo() {
		sendCommand("GET", "/v1/session/" + sessionID + "/player", null);
	}

	private void sendCommand(String method, String path, JSONObject payload) {
		APIManager rokuDevAPIManager = new APIManager(APIType.ROKU_WEBDRIVER, deviceIP, path, method);
		if (payload != null) {
			rokuDevAPIManager.addRequestPayload(payload);
		}
		rokuDevAPIManager.sendDevAPICommand();
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

	public JSONObject getResponseObj() {
		return responseObj;
	}

	public int getWebDriverResponseCode() {
		if (responseObj != null && responseObj.containsKey("status")) {
			return (int) (long) responseObj.get("status");
		}
		return -1;
	}

	public String getSessionID() {
		if (responseObj != null && responseObj.containsKey("sessionId")) {
			return (String) responseObj.get("sessionId");
		}
		return null;
	}

	private String getDeviceSessionID() {
		String cacheValue = CacheUtils.get(getSessionIDCacheProp());
		if (cacheValue != null) {
			return cacheValue;
		}

		JSONObject rokuInfo = info.getRokuDeviceInfo(deviceIP);
		if (rokuInfo != null && rokuInfo.containsKey("device-info")) {
			JSONObject deviceInfoObj = (JSONObject) rokuInfo.get("device-info");
			String sessionID = deviceInfoObj.get("advertising-id").toString();
			CacheUtils.add(getSessionIDCacheProp(), sessionID);
			return sessionID;
		}
		return null;
	}

	private String getSessionIDCacheProp() {
		return "roku-webdriver-session-id-" + deviceIP + "-cache-prop";
	}
}
