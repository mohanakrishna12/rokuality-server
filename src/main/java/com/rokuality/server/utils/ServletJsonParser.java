package com.rokuality.server.utils;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.ImageCollector;
import com.rokuality.server.core.drivers.SessionManager;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServletJsonParser {

	public static final String SERVLET_PAYLOAD = "servlet.payload";

	public JSONObject getRequestJSON(HttpServletRequest request, HttpServletResponse response) {

		String content = null;
		try {
			content = org.apache.commons.io.IOUtils.toString(request.getReader());
			
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to retrieve request content!");
			} catch (IOException e1) {
				Log.getRootLogger().warn(e1);
			}
			Log.getRootLogger().warn(e);
		}

		JSONParser jsonParser = new JSONParser();
		JSONObject jsonObject = null;

		if (content != null) {
			request.setAttribute(SERVLET_PAYLOAD, content);
			try {
				jsonObject = (JSONObject) jsonParser.parse(content);
			} catch (ParseException e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				try {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
							"Request content is not a valid json object!");
				} catch (IOException e1) {
					Log.getRootLogger().warn(e1);
				}
				Log.getRootLogger().warn(e);
			}
		}

		// TODO - get the sessionn id (if not a start activity) and check if the SessionManager contains it
		// otherwise it's a SessionNotActive event we should handle and return/throw to the user

		Object session = jsonObject.get(SessionConstants.SESSION_ID);
		if (session != null) {
			String sessionID = String.valueOf(session);
			SessionManager.updateSessionActivity(sessionID);
		}

		return jsonObject;
	}

}
