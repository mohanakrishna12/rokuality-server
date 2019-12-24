package com.rokuality.server.servlets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.drivers.ElementManager;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.driver.device.roku.RokuWebDriverAPIManager;
import com.rokuality.server.enums.RokuWebDriverLocatorType;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.ImageUtils;
import com.rokuality.server.utils.ServletJsonParser;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@SuppressWarnings({ "serial", "unchecked" })
public class element extends HttpServlet {

	private static final String BY_IMAGE_ID = "By.Image: ";
	private static final String BY_OCR_ID = "By.Text: ";
	private static final String BY_TEXT_ID = "RokuBy.Text: ";
	private static final String BY_TAG_ID = "RokuBy.Tag: ";
	private static final String BY_ATTRIBUTE_ID = "RokuBy.Attribute: ";
	private static final String ATTRIBUTE_VALUE_SPLITTER = "::::::::";

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		JSONObject requestObj = new ServletJsonParser().getRequestJSON(request, response);
		if (response.getStatus() != HttpServletResponse.SC_OK) {
			return;
		}

		JSONObject results = null;

		String action = requestObj.get(ServerConstants.SERVLET_ACTION).toString();
		switch (action) {
		case "find":
			results = findElement(requestObj, false);
			break;
		case "find_all":
			results = findElement(requestObj, true);
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

	public JSONObject findElement(JSONObject requestObj, boolean allowMultiple) {
		String sessionID = requestObj.get(SessionConstants.SESSION_ID).toString();
		String locator = requestObj.get(SessionConstants.ELEMENT_LOCATOR).toString();

		JSONObject element = new JSONObject();

		boolean locatorValidForSession = isLocatorValidForSession(sessionID, locator);
		if (!locatorValidForSession) {
			element.put(ServerConstants.SERVLET_RESULTS,
					"Your locator type is not valid! Only OCR or Image locators are supported for your session type.");
			return element;
		}

		Long subScreenX = null;
		Long subScreenY = null;
		Long subScreenWidth = null;
		Long subScreenHeight = null;
		if (!isRokuNativeLocator(locator) && requestObj.containsKey(SessionConstants.SUB_SCREEN_X)) {
			subScreenX = (Long) requestObj.getOrDefault(SessionConstants.SUB_SCREEN_X, null);
			subScreenY = (Long) requestObj.getOrDefault(SessionConstants.SUB_SCREEN_Y, null);
			subScreenWidth = (Long) requestObj.getOrDefault(SessionConstants.SUB_SCREEN_WIDTH, null);
			subScreenHeight = (Long) requestObj.getOrDefault(SessionConstants.SUB_SCREEN_HEIGHT, null);
		}

		List<JSONObject> elements = new ArrayList<>();

		boolean locatorIsImage = locator.startsWith(BY_IMAGE_ID);
		boolean locatorIsText = !locatorIsImage;

		List<String> locatorComponents = getLocator(locator);
		if (locatorComponents.isEmpty() || locatorComponents.size() < 2) {
			element.put(ServerConstants.SERVLET_RESULTS, "Your locator is not valid!");
			return element;
		}
		String locatorSplitter = locatorComponents.get(0);
		String locatorIdentifier = locatorComponents.get(1);

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
				elements = ImageUtils.getElementFromSubScreen(sessionID, loc, (long) subScreenX, (long) subScreenY,
						(long) subScreenWidth, (long) subScreenHeight);
			} else {
				if (isRokuNativeLocator(locator)) {
					JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);
					String deviceIP = (String) sessionInfo.get(SessionConstants.DEVICE_IP);
					RokuWebDriverAPIManager rokuWebDriverAPIManager = new RokuWebDriverAPIManager(deviceIP);
					String attribute = locatorComponents.size() == 3 ? locatorComponents.get(2) : null;
					rokuWebDriverAPIManager.findElements(getRokuLocatorType(locatorSplitter), attribute, loc);
					if (rokuWebDriverAPIManager.getWebDriverResponseCode() == 0) {
						elements = constructRokuNativeElements(rokuWebDriverAPIManager.getResponseObj());
					}
				} else {
					elements = ImageUtils.getElementFromEntireScreen(sessionID, loc);
				}
			}

			if (!elements.isEmpty()) {
				Log.getRootLogger().info("Element found: " + elements.toString());
				break;
			}

			SleepUtils.sleep(pollInterval);
		}

		if (locatorIsImage) {
			FileUtils.deleteFile(new File(loc));
		}

		// found multiple matches
		if (allowMultiple && !elements.isEmpty()) {
			JSONArray allElementArr = new JSONArray();
			for (JSONObject ele : elements) {
				allElementArr.add(ele);
			}
			element.put("all_elements", allElementArr);
		}

		// found singular match
		if (!allowMultiple && !elements.isEmpty()) {
			element = elements.get(0);
		}

		// if not allow multiple return an error object to throw exception
		if (!allowMultiple && element.isEmpty()) {
			String errorMsg = String.format(
					"Failed to find element with locator type %s and identifier %s on the device screen.",
					locatorSplitter, locatorIdentifier);
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

	private boolean isLocatorValidForSession(String sessionID, String locator) {
		if (!SessionManager.isRoku(sessionID)) {
			return locator.startsWith(BY_IMAGE_ID) || locator.startsWith(BY_OCR_ID);
		}

		return true;
	}

	private boolean isRokuNativeLocator(String locator) {
		return locator.startsWith(BY_TEXT_ID) || locator.startsWith(BY_TAG_ID) || locator.startsWith(BY_ATTRIBUTE_ID);
	}

	private List<String> getLocator(String locator) {
		List<String> locatorComponents = new ArrayList<>();
		String[] LOCATOR_SPLITTERS = { BY_IMAGE_ID, BY_TEXT_ID, BY_OCR_ID, BY_TAG_ID, BY_ATTRIBUTE_ID };
		List<String> splitters = Arrays.asList(LOCATOR_SPLITTERS);

		if (locator.startsWith(BY_ATTRIBUTE_ID) && locator.contains(ATTRIBUTE_VALUE_SPLITTER)) {
			locatorComponents.add(BY_ATTRIBUTE_ID);
			locatorComponents.add(locator.split(ATTRIBUTE_VALUE_SPLITTER)[1]);
			locatorComponents.add(locator.split(BY_ATTRIBUTE_ID)[1].split(ATTRIBUTE_VALUE_SPLITTER)[0]);
		} else {
			for (String splitter : splitters) {
				if (locator.startsWith(splitter)) {
					locatorComponents.add(splitter);
					try {
						locatorComponents.add(locator.split(splitter)[1]);
					} catch (Exception e) {
						Log.getRootLogger().warn(e);
						return new ArrayList<>();
					}
					break;
				}
			}
		}

		return locatorComponents;
	}

	private RokuWebDriverLocatorType getRokuLocatorType(String locatorSplitter) {
		switch (locatorSplitter) {
		case BY_TEXT_ID:
			return RokuWebDriverLocatorType.TEXT;
		case BY_TAG_ID:
			return RokuWebDriverLocatorType.TAG;
		case BY_ATTRIBUTE_ID:
			return RokuWebDriverLocatorType.ATTR;
		default:
			return RokuWebDriverLocatorType.TEXT;
		}
	}

	// TODO - cleanup redundancies
	public static List<JSONObject> constructRokuNativeElements(JSONObject elementObj) {
		List<JSONObject> elements = new ArrayList<>();

		JSONArray valueArr = (JSONArray) elementObj.get("value");

		if (valueArr == null) {
			return elements;
		}

		for (int i = 0; i < valueArr.size(); i++) {
			String[] boundsComponents = { "0", "0", "0", "0" };
			String text = "";

			JSONObject valueObj = (JSONObject) valueArr.get(i);
			JSONArray attrArr = (JSONArray) valueObj.get("Attrs");

			for (int i2 = 0; i2 < attrArr.size(); i2++) {
				JSONObject attrObj = (JSONObject) attrArr.get(i2);
				JSONObject nameObj = (JSONObject) attrObj.get("Name");
				if (nameObj.containsValue("bounds")) {
					String boundsStr = (String) attrObj.get("Value");
					boundsStr = boundsStr.replace("{", "").replace("}", "");
					boundsComponents = boundsStr.split(", ");
				}

				if (nameObj.containsValue("text")) {
					text = (String) attrObj.get("Value");
				}
			}

			elements.add(constructRokuNativeElementJSON(elementObj, text, boundsComponents));
		}

		return elements;
	}

	public static JSONObject constructRokuNativeElement(JSONObject elementObj) {
		JSONObject valueObj = (JSONObject) elementObj.get("value");

		String[] boundsComponents = { "0", "0", "0", "0" };
		String text = "";

		JSONArray attrArr = (JSONArray) valueObj.get("Attrs");

		for (int i = 0; i < attrArr.size(); i++) {
			JSONObject attrObj = (JSONObject) attrArr.get(i);
			JSONObject nameObj = (JSONObject) attrObj.get("Name");
			if (nameObj.containsValue("bounds")) {
				String boundsStr = (String) attrObj.get("Value");
				boundsStr = boundsStr.replace("{", "").replace("}", "");
				boundsComponents = boundsStr.split(", ");
			}

			if (nameObj.containsValue("text")) {
				text = (String) attrObj.get("Value");
			}
		}

		return constructRokuNativeElementJSON(elementObj, text, boundsComponents);

	}

	private static JSONObject constructRokuNativeElementJSON(JSONObject elementObj, String text,
			String[] boundsComponents) {
		JSONObject element = new JSONObject();
		element.put(SessionConstants.ELEMENT_ID, UUID.randomUUID().toString());
		element.put("element_x", getParsedBound(boundsComponents[0]));
		element.put("element_y", getParsedBound(boundsComponents[1]));
		element.put("element_width", getParsedBound(boundsComponents[2]));
		element.put("element_height", getParsedBound(boundsComponents[3]));
		element.put("element_confidence", 100);
		element.put("element_text", text);
		element.put("element_json", elementObj.toJSONString());
		return element;
	}

	private static int getParsedBound(String boundComponent) {
		try {
			return Integer.parseInt(boundComponent);
		} catch (Exception e) {
			return 0;
		}
	}

}
