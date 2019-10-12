package com.rokuality.server.servlets;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.driver.device.roku.RokuKeyPresser;
import com.rokuality.server.driver.device.xbox.XBoxKeyPresser;
import com.rokuality.server.enums.RokuButton;
import com.rokuality.server.enums.SpecialCharacters;
import com.rokuality.server.enums.XBoxButton;
import com.rokuality.server.utils.ServletJsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({ "serial", "unchecked" })
public class remote extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		JSONObject requestObj = new ServletJsonParser().getRequestJSON(request, response);

		JSONObject results = null;

		String action = requestObj.get(ServerConstants.SERVLET_ACTION).toString();
		switch (action) {
		case "press_button":
			results = pressButton(requestObj);
			break;
		case "send_keys":
			results = sendKeys(requestObj);
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

	public static JSONObject pressRokuRemoteButton(String sessionID, RokuButton rokuButton) {
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

	public static JSONObject pressXBoxRemoteButton(String sessionID, XBoxButton xboxButton) {
		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);

		JSONObject buttonObj = new JSONObject();
		if (sessionInfo == null) {
			buttonObj.put(ServerConstants.SERVLET_RESULTS,
					"No session found during button press for session " + String.valueOf(sessionID));
			return buttonObj;
		}

		String homeHubDeviceIP = (String) sessionInfo.get(SessionConstants.HOME_HUB_DEVICE_IP);
		String deviceName = (String) sessionInfo.get(SessionConstants.DEVICE_NAME);
		boolean success = false;
		try {
			success = new XBoxKeyPresser(homeHubDeviceIP, deviceName).pressButton(xboxButton);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		if (!success) {
			buttonObj = new JSONObject();
			buttonObj.put(ServerConstants.SERVLET_RESULTS,
					"Failed to send remote command " + xboxButton.value() + " to device!");
			return buttonObj;
		}

		buttonObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		return buttonObj;
	}

	public static JSONObject sendKeys(JSONObject requestObj) {
		String sessionID = requestObj.get(SessionConstants.SESSION_ID).toString();
		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);
		String text = (String) requestObj.get(SessionConstants.TEXT);

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

	private JSONObject pressButton(JSONObject requestObj) {
		JSONObject results = null;
		String sessionID = requestObj.get(SessionConstants.SESSION_ID).toString();

		if (SessionManager.isRoku(sessionID)) {
			RokuButton rokuButton = RokuButton
					.getEnumByString(requestObj.get(SessionConstants.REMOTE_BUTTON).toString());
			results = pressRokuRemoteButton(sessionID, rokuButton);
		}

		if (SessionManager.isXBox(sessionID)) {
			XBoxButton xboxButton = XBoxButton
					.getEnumByString(requestObj.get(SessionConstants.REMOTE_BUTTON).toString());
			results = pressXBoxRemoteButton(sessionID, xboxButton);
		}

		return results;
	}

}
