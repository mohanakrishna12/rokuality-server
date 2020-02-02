package com.rokuality.server.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.driver.device.roku.RokuKeyPresser;
import com.rokuality.server.enums.SessionStatus;
import com.rokuality.server.utils.ServletJsonParser;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

@SuppressWarnings({"serial", "unchecked"})
public class settings extends HttpServlet {
	
	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        JSONObject requestObj = new ServletJsonParser().getRequestJSON(request, response);
        if (response.getStatus() != HttpServletResponse.SC_OK) {
			return;
		}
		
        JSONObject results = null;

		String action = requestObj.get(ServerConstants.SERVLET_ACTION).toString();
		switch (action) {
			case SessionConstants.ELEMENT_FIND_TIMEOUT:
			results = setElementTimeout(requestObj);
			break;
			case SessionConstants.IMAGE_MATCH_SIMILARITY:
			results = setImageMatchSimilarity(requestObj);
			break;
			case "set_session_status":
			results = setSessionStatus(requestObj);
			break;
			case "get_session_status":
			results = getSessionStatus(requestObj);
			break;
			case SessionConstants.ELEMENT_POLLING_INTERVAL:
			results = setElementPollInterval(requestObj);
			break;
			case SessionConstants.REMOTE_INTERACT_DELAY:
			results = setRemoteInteractDelay(requestObj);
			break;
			case "reboot_device":
			results = rebootDevice(requestObj);
			break;
			default:

			break;
		}
        
		if (results != null && results.containsValue(ServerConstants.SERVLET_SUCCESS) ) {
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
        
        response.setContentType("application/json");
        response.getWriter().println(results.toJSONString());
    }

    public static JSONObject setElementTimeout(JSONObject sessionObj) {
		String sessionID = String.valueOf(sessionObj.get(SessionConstants.SESSION_ID));
		Long timeout = Long.parseLong(String.valueOf(sessionObj.get(SessionConstants.ELEMENT_FIND_TIMEOUT)));
		
		JSONObject resultObj = new JSONObject();

		SessionManager.updateSessionInfoItem(sessionID, SessionConstants.ELEMENT_FIND_TIMEOUT, timeout);
		resultObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		resultObj.put(SessionConstants.ELEMENT_FIND_TIMEOUT, String.valueOf(timeout));
		return resultObj;
	}
	
	public static JSONObject setImageMatchSimilarity(JSONObject sessionObj) {
		String sessionID = String.valueOf(sessionObj.get(SessionConstants.SESSION_ID));
		String imageMatchSimilarity = sessionObj.get(SessionConstants.IMAGE_MATCH_SIMILARITY).toString();

		JSONObject resultObj = new JSONObject();
		Log.getRootLogger().info(String.format("Setting image match similarity to %s for session %s", Double.parseDouble(imageMatchSimilarity), sessionID));

		SessionManager.updateSessionInfoItem(sessionID, SessionConstants.IMAGE_MATCH_SIMILARITY, Double.parseDouble(imageMatchSimilarity));
		resultObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		resultObj.put(SessionConstants.IMAGE_MATCH_SIMILARITY, String.valueOf(imageMatchSimilarity));
		return resultObj;
	}

	public static JSONObject setSessionStatus(JSONObject sessionObj) {
		String sessionID = String.valueOf(sessionObj.get(SessionConstants.SESSION_ID));
		SessionStatus status = SessionStatus.getEnumByString((String) sessionObj.get(SessionConstants.SESSION_STATUS));

		JSONObject resultObj = new JSONObject();
		Log.getRootLogger().info(String.format("Setting session status to %s for session %s", status.value(), sessionID));

		SessionManager.updateSessionInfoItem(sessionID, SessionConstants.SESSION_STATUS, status.value());
		resultObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		resultObj.put(SessionConstants.SESSION_STATUS, status.value());
		return resultObj;
	}

	public static JSONObject getSessionStatus(JSONObject sessionObj) {
		String sessionID = String.valueOf(sessionObj.get(SessionConstants.SESSION_ID));
		
		JSONObject resultObj = new JSONObject();

		String status = (String) SessionManager.getSessionInfo(sessionID).get(SessionConstants.SESSION_STATUS);
		if (status == null) {
			status = SessionStatus.IN_PROGRESS.value();
		}
		resultObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		resultObj.put(SessionConstants.SESSION_STATUS, status);
		return resultObj;
	}
	
	public static JSONObject setElementPollInterval(JSONObject sessionObj) {
		String sessionID = String.valueOf(sessionObj.get(SessionConstants.SESSION_ID));
		Long pollInterval = Long.parseLong(String.valueOf(sessionObj.get(SessionConstants.ELEMENT_POLLING_INTERVAL)));
		
		JSONObject resultObj = new JSONObject();

		SessionManager.updateSessionInfoItem(sessionID, SessionConstants.ELEMENT_POLLING_INTERVAL, pollInterval);
		resultObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		resultObj.put(SessionConstants.ELEMENT_POLLING_INTERVAL, String.valueOf(pollInterval));
		return resultObj;
	}

	public static JSONObject setRemoteInteractDelay(JSONObject sessionObj) {
		String sessionID = String.valueOf(sessionObj.get(SessionConstants.SESSION_ID));
		JSONObject resultObj = new JSONObject();
		int delay = 0;
		try {
			delay = Integer.parseInt(String.valueOf(sessionObj.get(SessionConstants.REMOTE_INTERACT_DELAY)));
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
			resultObj.put(ServerConstants.SERVLET_RESULTS, "Failed to parse remote interaction delay. Must be an integer greater than zero!");
			return resultObj;
		}
		
		Log.getRootLogger().info(String.format("Setting remote control interact delay to %s milliseconds.", delay));
		SessionManager.updateSessionInfoItem(sessionID, SessionConstants.REMOTE_INTERACT_DELAY, delay);
		resultObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		resultObj.put(SessionConstants.ELEMENT_POLLING_INTERVAL, String.valueOf(delay));
		return resultObj;
	}

	public static JSONObject rebootDevice(JSONObject sessionObj) {
		String sessionID = String.valueOf(sessionObj.get(SessionConstants.SESSION_ID));

		JSONObject results = new JSONObject();

		if (!SessionManager.isRoku(sessionID)) {
			results.put(ServerConstants.SERVLET_RESULTS, "Reboot only available for Roku.");
			return results;
		}

		String deviceIP = (String) SessionManager.getSessionInfo(sessionID).get(SessionConstants.DEVICE_IP);
		try {
			Log.getRootLogger().info(String.format("Rebooting device %s", deviceIP));
			RokuKeyPresser.rebootRoku(deviceIP);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);

		return results;
	}

}

