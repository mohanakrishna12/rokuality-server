package com.rokuality.server.utils;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.drivers.SessionManager;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
public class ServletJsonParser {

	public static final String SERVLET_PAYLOAD = "servlet.payload";

	public JSONObject getRequestJSON(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSONObject jsonObject = null;
		JSONObject errorJSONObj = (JSONObject) new JSONObject();
		try {
			String content = org.apache.commons.io.IOUtils.toString(request.getReader());
			request.setAttribute(SERVLET_PAYLOAD, content);
			jsonObject = (JSONObject) new JSONParser().parse(content);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			errorJSONObj.put(ServerConstants.SERVLET_RESULTS, "Failed to process request body.");
			response.getWriter().println(errorJSONObj.toJSONString());
			return null;
		}

		String sessionID = (String) jsonObject.get(SessionConstants.SESSION_ID);
		if (sessionID != null) {
			SessionManager.updateSessionActivity(sessionID);
		}

		if (sessionID != null && !SessionManager.isSessionInitiated(sessionID) && !request.getRequestURI().contains("session")) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			errorJSONObj.put(ServerConstants.SERVLET_RESULTS, "No active session found for " + sessionID);
			response.getWriter().println(errorJSONObj.toJSONString());
			return null;
		}

		return jsonObject;
	}

}
