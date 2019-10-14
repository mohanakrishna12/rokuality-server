package com.rokuality.server.servlets;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.drivers.ElementManager;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.ImageUtils;
import com.rokuality.server.utils.ServletJsonParser;
import com.rokuality.server.utils.SleepUtils;

import java.io.File;
import java.io.IOException;

@SuppressWarnings({ "serial", "unchecked" })
public class element extends HttpServlet {

	private static final String BY_IMAGE_ID = "By.Image: ";
	private static final String BY_TEXT_ID = "By.Text: ";

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		JSONObject requestObj = new ServletJsonParser().getRequestJSON(request, response);
		JSONObject results = null;

		String action = requestObj.get(ServerConstants.SERVLET_ACTION).toString();
		switch (action) {
		case "find":
			results = findElement(requestObj);
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

	public JSONObject findElement(JSONObject requestObj) {
		String sessionID = requestObj.get(SessionConstants.SESSION_ID).toString();
		String locator = requestObj.get(SessionConstants.ELEMENT_LOCATOR).toString();

		Long subScreenX = null;
		Long subScreenY = null;
		Long subScreenWidth = null;
		Long subScreenHeight = null;
		if (requestObj.containsKey(SessionConstants.SUB_SCREEN_X)) {
			subScreenX = (Long) requestObj.getOrDefault(SessionConstants.SUB_SCREEN_X, null);
			subScreenY = (Long) requestObj.getOrDefault(SessionConstants.SUB_SCREEN_Y, null);
			subScreenWidth = (Long) requestObj.getOrDefault(SessionConstants.SUB_SCREEN_WIDTH, null);
			subScreenHeight = (Long) requestObj.getOrDefault(SessionConstants.SUB_SCREEN_HEIGHT, null);
		}

		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);

		JSONObject element = new JSONObject();
		if (sessionInfo == null) {
			element.put(ServerConstants.SERVLET_RESULTS,
					"No session found during element find for session " + String.valueOf(sessionID));
			return element;
		}

		boolean locatorIsText = locator.startsWith(BY_TEXT_ID);
		boolean locatorIsImage = !locatorIsText;

		String locatorSplitter = locatorIsText ? BY_TEXT_ID : BY_IMAGE_ID;
		String locatorIdentifier = null;
		try {
			locatorIdentifier = locator.split(locatorSplitter)[1];
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		if (locatorIdentifier == null || locatorIdentifier.isEmpty()) {
			element.put(ServerConstants.SERVLET_RESULTS,
					"Your locator is not valid! If text it should be a valid word or "
							+ "string of words to search for on the device screen. If an image, it should be a valid path "
							+ "to an image OR a valid URL to an image.");
			return element;
		}

		String loc = null;
		if (locatorIsImage) {
			try {
				loc = ImageUtils.imageHandler(locatorIdentifier);
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}

			if (loc == null) {
				element.put(ServerConstants.SERVLET_RESULTS, "Failed to decode image element during element find. "
						+ "Is your image element a proper url or path to a valid image file?");
				return element;
			}
		}

		if (locatorIsText) {
			loc = locatorIdentifier;
		}

		int pollInterval = getPollingInterval(sessionID);
		Long duration = (long) SessionManager.getSessionInfo(sessionID).get(SessionConstants.ELEMENT_FIND_TIMEOUT);
		Log.getRootLogger().info("Element duration: " + duration);

		long pollStart = System.currentTimeMillis();
		long pollMax = pollStart + (duration);

		while (System.currentTimeMillis() <= pollMax) {
			if (subScreenX != null && subScreenY != null && subScreenWidth != null && subScreenHeight != null) {
				element = ImageUtils.getElementFromSubScreen(sessionID, loc, (long) subScreenX, (long) subScreenY,
						(long) subScreenWidth, (long) subScreenHeight);
			} else {
				element = ImageUtils.getElementFromEntireScreen(sessionID, loc);
			}

			if (element != null) {
				Log.getRootLogger().info("Element found: " + element.toJSONString());
				break;
			}

			SleepUtils.sleep(pollInterval);
		}

		if (locatorIsImage) {
			FileUtils.deleteFile(new File(loc));
		}

		if (element == null) {
			element = new JSONObject();
			// TODO - better custom error message based on what ocr engine or element type
			// is being provided
			String eleTypeStr = locatorIsText ? "text" : "image";
			String errorMsg = "Failed to find " + eleTypeStr
					+ " element on the device screen! If you believe the element to be present "
					+ "try increasing the element timeout, reducing the image match similarity, "
					+ "or using the Google Vision OCR module instead of Tesseract.";
			element.put(ServerConstants.SERVLET_RESULTS, errorMsg);
			return element;
		}

		element.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		element.put(SessionConstants.SESSION_ID, sessionID);
		ElementManager.addElement(sessionID, element);

		return element;
	}

	private int getPollingInterval(String sessionID) {
		Object pollObj = SessionManager.getSessionInfo(sessionID).get(SessionConstants.ELEMENT_POLLING_INTERVAL);
		if (pollObj == null) {
			return SessionConstants.DEFAULT_ELEMENT_POLL_INTERVAL_MS;
		}

		Long interval = (long) pollObj;
		return interval.intValue();
	}

}
