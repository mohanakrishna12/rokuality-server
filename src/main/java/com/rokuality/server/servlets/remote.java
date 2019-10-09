package com.rokuality.server.servlets;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.driver.device.RokuKeyPresser;
import com.rokuality.server.enums.RokuButton;
import com.rokuality.server.enums.SpecialCharacters;
import com.rokuality.server.utils.ServletJsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({ "serial", "unchecked" })
public class remote extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		JSONObject requestObj = new ServletJsonParser().getRequestJSON(request, response);
		String sessionID = requestObj.get(SessionConstants.SESSION_ID).toString();

		JSONObject results = null;

		String action = requestObj.get(ServerConstants.SERVLET_ACTION).toString();
		switch (action) {
		case "press_button":
			RokuButton rokuButton = RokuButton
					.getEnumByString(requestObj.get(SessionConstants.REMOTE_BUTTON).toString());
			results = pressRemoteButton(sessionID, rokuButton);
			break;
		case "send_keys":
			String textToSend = (String) requestObj.get(SessionConstants.TEXT);
			results = sendKeys(sessionID, textToSend);
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

	public static JSONObject pressRemoteButton(String sessionID, RokuButton rokuButton) {
		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);

		JSONObject buttonObj = new JSONObject();
		if (sessionInfo == null) {
			buttonObj.put(ServerConstants.SERVLET_RESULTS,
					"No session found during button press for session " + String.valueOf(sessionID));
			return buttonObj;
		}

		String deviceIP = (String) sessionInfo.get(SessionConstants.DEVICE_IP);
		boolean success = false;
		try {
			String deviceButton = RokuButton.getDeviceButton(rokuButton);
			success = RokuKeyPresser.rokuKeyPresser(deviceIP, deviceButton);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		if (!success) {
			buttonObj = new JSONObject();
			buttonObj.put(ServerConstants.SERVLET_RESULTS,
					"Failed to send remote command " + rokuButton.value() + " to device!");
			return buttonObj;
		}

		buttonObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		return buttonObj;
	}

	public static JSONObject sendKeys(String sessionID, String text) {
		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);

		JSONObject buttonObj = new JSONObject();
		if (sessionInfo == null) {
			buttonObj.put(ServerConstants.SERVLET_RESULTS,
					"No session found during button press for session " + String.valueOf(sessionID));
			return buttonObj;
		}

		String deviceIP = (String) sessionInfo.get(SessionConstants.DEVICE_IP);

		List<Boolean> keySendSuccess = new ArrayList<>();
		for (int i = 0; i < text.length(); i++) {
			boolean success = false;
			char c = text.charAt(i);
			try {
				if (SpecialCharacters.isKnownSymbol(c)) {
					success = RokuKeyPresser.rokuKeyPresser(deviceIP, "Lit_" + SpecialCharacters.getCodeBySymbol(c));
				} else {
					success = RokuKeyPresser.rokuKeyPresser(deviceIP, "Lit_" + c);
				}
			} catch (Exception e) {
				buttonObj.put(ServerConstants.SERVLET_RESULTS,
						String.format("Failed to send literal character %s to device.", c));
				return buttonObj;
			}
			keySendSuccess.add(success);
		}

		if (keySendSuccess.contains(false)) {
			buttonObj.put(ServerConstants.SERVLET_RESULTS,
					String.format("Failed to send literal characters %s to device.", String.valueOf(text)));
			return buttonObj;
		}

		buttonObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		return buttonObj;
	}

}
