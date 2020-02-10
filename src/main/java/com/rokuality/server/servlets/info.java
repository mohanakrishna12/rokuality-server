package com.rokuality.server.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.driver.host.APIManager;
import com.rokuality.server.driver.device.roku.RokuLogManager;
import com.rokuality.server.driver.device.roku.RokuPackageHandler;
import com.rokuality.server.driver.device.roku.RokuProfilerManager;
import com.rokuality.server.driver.device.roku.RokuWebDriverAPIManager;
import com.rokuality.server.driver.device.xbox.XBoxDevAPIManager;
import com.rokuality.server.enums.APIType;
import com.rokuality.server.enums.SessionCapabilities;
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
		case "media_player_info":
			results = getRokuMediaPlayerInfo(sessionID);
			break;
		case "performance_profile":
			results = getRokuPerformanceProfile(sessionID);
			break;
		case "get_active_app":
			results = getActiveApp(sessionID);
			break;
		case "get_installed_apps":
			results = getInstalledApps(sessionID);
			break;
		case "get_debug_logs":
			results = getDebugLogs(sessionID);
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

		APIManager rokuDevAPIManager = new APIManager(APIType.ROKU_DEV_API, ipAddress, "/query/device-info", "GET");
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
			results.put(ServerConstants.SERVLET_RESULTS,
					String.format("Failed to parse device-info with output %s", output));
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
			results.put(ServerConstants.SERVLET_RESULTS,
					String.format("Failed to parse device info with output %s", output));
			return results;
		}

		return results;
	}

	public static JSONObject getRokuMediaPlayerInfo(String sessionID) {
		JSONObject results = new JSONObject();

		String deviceIP = (String) SessionManager.getSessionInfo(sessionID).get(SessionConstants.DEVICE_IP);
		RokuWebDriverAPIManager rokuWebDriverAPIManager = new RokuWebDriverAPIManager(deviceIP);
		rokuWebDriverAPIManager.getMediaPlayerInfo();
		if (rokuWebDriverAPIManager.getWebDriverResponseCode() != 0) {
			results.put(ServerConstants.SERVLET_RESULTS,
					"Failed to retrieve media player info! Is the media player open?");
			return results;
		}

		results = rokuWebDriverAPIManager.getResponseObj();
		results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);

		return results;
	}

	public static JSONObject getRokuPerformanceProfile(String sessionID) {
		JSONObject results = new JSONObject();

		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);
		String deviceIP = (String) sessionInfo.get(SessionConstants.DEVICE_IP);
		boolean isProfileCapture = (boolean) sessionInfo.getOrDefault(SessionConstants.PROFILE_CAPTURE_STARTED, false);

		if (!isProfileCapture) {
			results.put(ServerConstants.SERVLET_RESULTS,
					String.format(
							"Roku performance profiling is not enabled on device %s. "
									+ "To enable please set the %s capability to true prior to session start.",
							deviceIP, SessionCapabilities.ENABLE_PERFORMANCE_PROFILING.value()));
			return results;
		}

		String processedProfileData = RokuProfilerManager.processProfileData(deviceIP);
		if (processedProfileData == null) {
			results.put(ServerConstants.SERVLET_RESULTS,
					String.format("Failed to process performance profiling data on device %s", deviceIP));
			return results;
		}

		results.put("performance_profiling_data", processedProfileData);
		results.put("performance_profile_file_ext", ".bsprof");
		results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);

		return results;
	}

	public static JSONObject getActiveApp(String sessionID) {
		JSONObject results = new JSONObject();

		String deviceIP = (String) SessionManager.getSessionInfo(sessionID).get(SessionConstants.DEVICE_IP);
		String activeApp = RokuPackageHandler.getActiveApp(deviceIP);
		if (activeApp == null || !activeApp.contains("app")) {
			results.put(ServerConstants.SERVLET_RESULTS, "Failed to retrieve active app info.");
			return results;
		}

		try {
			org.json.JSONObject xmlJSONObj = XML.toJSONObject(activeApp);
			String xmlToJSON = xmlJSONObj.toString(4);
			results = (JSONObject) new JSONParser().parse(xmlToJSON);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
			results.put(ServerConstants.SERVLET_RESULTS,
					String.format("Failed to parse device active app output with output %s", activeApp));
			return results;
		}

		results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);

		return results;
	}

	public static JSONObject getInstalledApps(String sessionID) {
		JSONObject results = new JSONObject();

		String deviceIP = (String) SessionManager.getSessionInfo(sessionID).get(SessionConstants.DEVICE_IP);
		String installedApps = RokuPackageHandler.getInstalledApps(deviceIP);
		if (installedApps == null || !installedApps.contains("apps")) {
			results.put(ServerConstants.SERVLET_RESULTS, "Failed to retrieve installed app info.");
			return results;
		}

		try {
			org.json.JSONObject xmlJSONObj = XML.toJSONObject(installedApps);
			String xmlToJSON = xmlJSONObj.toString(4);
			results = (JSONObject) new JSONParser().parse(xmlToJSON);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
			results.put(ServerConstants.SERVLET_RESULTS,
					String.format("Failed to parse device installed app output with output %s", installedApps));
			return results;
		}

		results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);

		return results;
	}

	public static JSONObject getDebugLogs(String sessionID) {
		JSONObject results = new JSONObject();

		if (!SessionManager.isRoku(sessionID)) {
			results.put(ServerConstants.SERVLET_RESULTS, "Debug logs only available for Roku.");
			return results;
		}

		String deviceIP = (String) SessionManager.getSessionInfo(sessionID).get(SessionConstants.DEVICE_IP);
		String logContent = "";
		try {
			logContent = RokuLogManager.getLogContent(deviceIP);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		if (logContent == null) {
			results.put(ServerConstants.SERVLET_RESULTS, "Failed to retrieve Roku debug logs.");
			return results;
		}

		results.put("log_content", logContent);
		results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);

		return results;
	}

}
