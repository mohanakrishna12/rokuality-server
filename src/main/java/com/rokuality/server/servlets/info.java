package com.rokuality.server.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.driver.device.roku.RokuDevAPIManager;
import com.rokuality.server.driver.device.xbox.XBoxDevAPIManager;
import com.rokuality.server.enums.RokuAPIType;
import com.rokuality.server.utils.ServletJsonParser;

import org.eclipse.jetty.util.log.Log;
import org.json.XML;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings({ "serial", "unchecked" })
public class info extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		JSONObject requestObj = new ServletJsonParser().getRequestJSON(request, response);
		if (response.getStatus() != HttpServletResponse.SC_OK) {
			return;
		}
		
        String sessionID = requestObj.get(SessionConstants.SESSION_ID).toString();
		
		JSONObject results = null;

		String action = requestObj.get(ServerConstants.SERVLET_ACTION).toString();
		switch (action) {
			case "device_info":
			results = getDeviceInfo(sessionID);
			break;
			default:

			break;
		}

        if (results != null && results.containsValue(ServerConstants.SERVLET_SUCCESS)) {
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		response.setContentType("application/json");
		response.getWriter().println(results.toJSONString());
        
    }

    public static JSONObject getDeviceInfo(String sessionID) {
        String deviceIp = (String) SessionManager.getSessionInfo(sessionID).get(SessionConstants.DEVICE_IP);
		
		JSONObject deviceInfoObj = new JSONObject();
		
		if (SessionManager.isRoku(sessionID)) {
			deviceInfoObj = getRokuDeviceInfo(deviceIp);
		}

		if (SessionManager.isXBox(sessionID)) {
			deviceInfoObj = getXBoxDeviceInfo(deviceIp);
		}

        return deviceInfoObj;
    }

    public static JSONObject getRokuDeviceInfo(String ipAddress) {
		JSONObject results = new JSONObject();

		RokuDevAPIManager rokuDevAPIManager = new RokuDevAPIManager(RokuAPIType.DEV_API, ipAddress, "/query/device-info", "GET");
		boolean success = rokuDevAPIManager.sendDevAPICommand();
		String output = rokuDevAPIManager.getResponseContent();

		if (output == null || output.isEmpty() || !success) {
			results.put(ServerConstants.SERVLET_RESULTS, "Failed to retrieve device info!");
			return results;
		}

		try {
			org.json.JSONObject xmlJSONObj = XML.toJSONObject(output);
			String xmlToJSON = xmlJSONObj.toString(4);
			results = (JSONObject) new JSONParser().parse(xmlToJSON);
			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
			results.put(ServerConstants.SERVLET_RESULTS, String.format("Failed to parse device-info with output %s", output));
			return results;
		}
		
		return results;
	}

	public static JSONObject getXBoxDeviceInfo(String ipAddress) {
		JSONObject results = new JSONObject();

		String output = new XBoxDevAPIManager(ipAddress).getDeviceInfo();
		
		if (output == null || output.isEmpty()) {
			results.put(ServerConstants.SERVLET_RESULTS, "Failed to retrieve device info!");
			return results;
		}

		try {
			results = (JSONObject) new JSONParser().parse(output);
			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
			results.put(ServerConstants.SERVLET_RESULTS, String.format("Failed to parse device info with output %s", output));
			return results;
		}
		
		return results;
	}

}
