package com.rokuality.server.servlets;

import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.utils.ServletJsonParser;

import java.io.IOException;

@SuppressWarnings({"serial", "unchecked"})
public class settings extends HttpServlet {
	
	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        JSONObject requestObj = new ServletJsonParser().getRequestJSON(request, response);
        
        JSONObject results = null;

		String action = requestObj.get(ServerConstants.SERVLET_ACTION).toString();
		switch (action) {
			case SessionConstants.ELEMENT_FIND_TIMEOUT:
			results = setElementTimeout(requestObj);
			break;
			case SessionConstants.IMAGE_MATCH_SIMILARITY:
			results = setImageMatchSimilarity(requestObj);
			break;

			case SessionConstants.ELEMENT_POLLING_INTERVAL:
			results = setElementPollInterval(requestObj);
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
		
		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);
		
		JSONObject resultObj = new JSONObject();
		if (sessionInfo == null) {
			resultObj.put(ServerConstants.SERVLET_RESULTS, "No session found during element timeout set for session "
				+ String.valueOf(sessionID));
			return resultObj;
		}

		SessionManager.updateSessionInfoItem(sessionID, SessionConstants.ELEMENT_FIND_TIMEOUT, timeout);
		resultObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		resultObj.put(SessionConstants.ELEMENT_FIND_TIMEOUT, String.valueOf(timeout));
		return resultObj;
	}
	
	public static JSONObject setImageMatchSimilarity(JSONObject sessionObj) {
		String sessionID = String.valueOf(sessionObj.get(SessionConstants.SESSION_ID));
		String imageMatchSimilarity = sessionObj.get(SessionConstants.IMAGE_MATCH_SIMILARITY).toString();

		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);
		
		JSONObject resultObj = new JSONObject();
		if (sessionInfo == null) {
			resultObj.put(ServerConstants.SERVLET_RESULTS, "No session found during element image match similarity set for session "
				+ String.valueOf(sessionID));
			return resultObj;
		}

		SessionManager.updateSessionInfoItem(sessionID, SessionConstants.IMAGE_MATCH_SIMILARITY, Double.parseDouble(imageMatchSimilarity));
		resultObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		resultObj.put(SessionConstants.IMAGE_MATCH_SIMILARITY, String.valueOf(imageMatchSimilarity));
		return resultObj;
	}
	
	public static JSONObject setElementPollInterval(JSONObject sessionObj) {
		String sessionID = String.valueOf(sessionObj.get(SessionConstants.SESSION_ID));
		Long pollInterval = Long.parseLong(String.valueOf(sessionObj.get(SessionConstants.ELEMENT_POLLING_INTERVAL)));
		
		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);
		
		JSONObject resultObj = new JSONObject();
		if (sessionInfo == null) {
			resultObj.put(ServerConstants.SERVLET_RESULTS, "No session found during element polling set for session "
				+ String.valueOf(sessionID));
			return resultObj;
		}

		SessionManager.updateSessionInfoItem(sessionID, SessionConstants.ELEMENT_POLLING_INTERVAL, pollInterval);
		resultObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		resultObj.put(SessionConstants.ELEMENT_POLLING_INTERVAL, String.valueOf(pollInterval));
		return resultObj;
	}

}

