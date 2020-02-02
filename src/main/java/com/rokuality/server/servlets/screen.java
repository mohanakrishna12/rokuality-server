package com.rokuality.server.servlets;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.awt.Dimension;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.ImageCollector;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.core.ocr.ImageText;
import com.rokuality.server.driver.device.roku.RokuWebDriverAPIManager;
import com.rokuality.server.enums.SessionCapabilities;
import com.rokuality.server.utils.FileToStringUtils;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.ImageUtils;
import com.rokuality.server.utils.ServletJsonParser;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

@SuppressWarnings({ "serial", "unchecked" })
public class screen extends HttpServlet {
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		JSONObject requestObj = new ServletJsonParser().getRequestJSON(request, response);
		if (response.getStatus() != HttpServletResponse.SC_OK) {
			return;
		}

		JSONObject results = null;

		String action = requestObj.get(ServerConstants.SERVLET_ACTION).toString();
		switch (action) {
		case "get_screen_image":
			results = getScreenImage(requestObj);
			break;
		case "get_screen_text":
			results = getScreenText(requestObj);
			break;
		case "get_screen_size":
			results = getScreenSize(requestObj);
			break;
		case "get_screen_recording":
			results = getScreenRecording(requestObj);
			break;
		case "get_screen_source":
			results = getScreenSource(requestObj);
			break;
		case "get_active_element":
			results = getActiveElement(requestObj);
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

	public static JSONObject getScreenImage(JSONObject sessionObj) {
		String sessionID = sessionObj.get(SessionConstants.SESSION_ID).toString();
		JSONObject results = new JSONObject();

		boolean useOriginalImage = (boolean) sessionObj.getOrDefault("use_original_image", false);

		File imageFile = collectImage(sessionID, sessionObj);

		String imageContent = null;
		if (imageFile != null && imageFile.exists()) {
			imageContent = new FileToStringUtils().convertToString(imageFile);
		}

		if (imageContent != null) {
			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
			results.put("screen_image", imageContent);
			results.put("screen_image_extension", ImageUtils.getImageFormat(imageFile));
		}

		if (imageContent == null) {
			results.put(ServerConstants.SERVLET_RESULTS, "Failed to get screen image/subimage!");
		}

		if (!useOriginalImage) {
			FileUtils.deleteFile(imageFile);
		}
		
		return results;
	}

	public static JSONObject getScreenText(JSONObject sessionObj) {
		JSONObject results = new JSONObject();
		String sessionID = sessionObj.get(SessionConstants.SESSION_ID).toString();

		File image = collectImage(sessionID, sessionObj);
		File resizedImageFile = null;

		Object resizeObj = SessionManager.getSessionInfo(sessionID).get(SessionConstants.SCREEN_SIZE_OVERRIDE);
		String defaultResize = String.valueOf(resizeObj);
		if (!defaultResize.equals("null")) {
			Log.getRootLogger().info(
					"Resizing screen text image '" + image.getAbsolutePath() + "' to size '" + defaultResize + "'.");

			String[] size = null;
			try {
				size = defaultResize.toLowerCase().replace(" ", "").split("x");
			} catch (Exception e) {
				results.put(ServerConstants.SERVLET_RESULTS,
						String.format(
								"The provided %s capability with value %s is "
										+ "not in the format 'widthxheight'. A working example would be '2400x1800'.",
								SessionCapabilities.SCREEN_SIZE_OVERRIDE.value(), defaultResize));
				return results;
			}

			resizedImageFile = ImageUtils.resizeImage(image, Integer.parseInt(size[0]), Integer.parseInt(size[1]));
			if (resizedImageFile != null && resizedImageFile.exists()) {
				com.rokuality.server.utils.FileUtils.deleteFile(image);
				image = resizedImageFile;
			}
		}

		List<ImageText> imageTexts = null;
		if (image != null && image.exists()) {
			imageTexts = ImageUtils.getTextsListFromImage(SessionManager.getOCRType(sessionID), image,
					SessionManager.getGoogleCredentials(sessionID));
		}

		if (image == null || !image.exists()) {
			results.put(ServerConstants.SERVLET_RESULTS, "Failed to capture image during screen evaluation!");
			return results;
		}

		String preparedScreenJSON = null;
		try {
			preparedScreenJSON = new Gson().toJson(imageTexts);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
			results.put(ServerConstants.SERVLET_RESULTS, "Failed to parse device screen text!");
		}

		if (imageTexts != null && preparedScreenJSON != null) {
			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
			results.put("screen_text", preparedScreenJSON);
		}

		FileUtils.deleteFile(image);
		return results;
	}

	public static JSONObject getScreenSize(JSONObject sessionObj) {
		JSONObject results = new JSONObject();
		String sessionID = sessionObj.get(SessionConstants.SESSION_ID).toString();

		ImageCollector imageCollector = SessionManager.getImageCollector(sessionID);
		File image = null;

		if (imageCollector != null) {
			image = imageCollector.getCurrentImage(true);
		}

		if (image == null || !image.exists() || !image.isFile()) {
			results.put(ServerConstants.SERVLET_RESULTS, "Failed to get device screen size!");
			return results;
		}

		Dimension screenSize = ImageUtils.getImageSize(image);
		int width = 0;
		int height = 0;
		if (screenSize != null) {
			width = screenSize.width;
			height = screenSize.height;
		}

		if (width != 0 && height != 0) {
			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
			results.put("screen_width", width);
			results.put("screen_height", height);
		}

		FileUtils.deleteFile(image);
		return results;
	}

	public static JSONObject getScreenRecording(JSONObject sessionObj) {
		JSONObject results = new JSONObject();
		String sessionID = sessionObj.get(SessionConstants.SESSION_ID).toString();

		if (SessionManager.isHDMI(sessionID)) {
			results = getHDMIScreenRecording(sessionID);
		} else {
			results = getImageCompiledScreenRecording(sessionID);
		}

		if (!results.containsValue(ServerConstants.SERVLET_SUCCESS)) {
			results.put(ServerConstants.SERVLET_RESULTS,
					"Failed to generate/retrieve video recording! See logs for details.");
		}
		return results;
	}

	public static JSONObject getScreenSource(JSONObject sessionObj) {
		JSONObject results = new JSONObject();
		String sessionID = sessionObj.get(SessionConstants.SESSION_ID).toString();

		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);
		String deviceIP = (String) sessionInfo.get(SessionConstants.DEVICE_IP);

		RokuWebDriverAPIManager rokuWebDriverAPIManager = new RokuWebDriverAPIManager(deviceIP);
		String xmlSource = rokuWebDriverAPIManager.getSource();

		if (xmlSource == null) {
			results.put(ServerConstants.SERVLET_RESULTS, "Failed to capture screen source!");
			return results;
		}

		results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		results.put("source", xmlSource);

		return results;
	}

	public static JSONObject getActiveElement(JSONObject sessionObj) {
		JSONObject results = new JSONObject();
		String sessionID = sessionObj.get(SessionConstants.SESSION_ID).toString();

		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);
		String deviceIP = (String) sessionInfo.get(SessionConstants.DEVICE_IP);

		RokuWebDriverAPIManager rokuWebDriverAPIManager = new RokuWebDriverAPIManager(deviceIP);
		rokuWebDriverAPIManager.getActiveElement();
		JSONObject elementObj = rokuWebDriverAPIManager.getResponseObj();
		elementObj = element.constructRokuNativeElement(elementObj);

		results = elementObj;
		results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);

		return results;
	}

	private static JSONObject getHDMIScreenRecording(String sessionID) {
		JSONObject results = new JSONObject();
		String videoContent = null;
		File capturedVideo = new File(
				String.valueOf(SessionManager.getSessionInfo(sessionID).get(SessionConstants.VIDEO_CAPTURE_FILE)));

		if (capturedVideo.exists() && capturedVideo.isFile()) {
			videoContent = new FileToStringUtils().convertToString(capturedVideo);
		}

		if (videoContent != null) {
			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
			results.put("screen_video", videoContent);
			results.put("screen_video_extension", ".mkv");
		}

		return results;
	}

	private static JSONObject getImageCompiledScreenRecording(String sessionID) {
		JSONObject results = new JSONObject();
		File video = null;
		String videoContent = null;
		ImageCollector imageCollector = SessionManager.getImageCollector(sessionID);
		if (imageCollector != null) {
			imageCollector.prepareVideo();
			video = imageCollector.getRecording();
		}

		if (video.exists() && video.isFile()) {
			videoContent = new FileToStringUtils().convertToString(video);
		}
		FileUtils.deleteFile(video);

		if (videoContent != null) {
			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
			results.put("screen_video", videoContent);
			results.put("screen_video_extension", ".mp4");
		}
		return results;
	}

	private static File collectImage(String sessionID, JSONObject sessionObj) {
		Object subScreenX = sessionObj.getOrDefault(SessionConstants.SUB_SCREEN_X, null);
		Object subScreenY = sessionObj.getOrDefault(SessionConstants.SUB_SCREEN_Y, null);
		Object subScreenWidth = sessionObj.getOrDefault(SessionConstants.SUB_SCREEN_WIDTH, null);
		Object subScreenHeight = sessionObj.getOrDefault(SessionConstants.SUB_SCREEN_HEIGHT, null);

		boolean useOriginalImage = (boolean) sessionObj.getOrDefault("use_original_image", false);

		ImageCollector imageCollector = SessionManager.getImageCollector(sessionID);
		File image = null;
		File subImageFile = null;
		if (imageCollector != null) {
			long pollStart = System.currentTimeMillis();
			long pollMax = pollStart + (5 * 1000);

			while (System.currentTimeMillis() <= pollMax) {
				image = useOriginalImage ? imageCollector.viewCurrentImage(true) : imageCollector.getCurrentImage(false);

				Log.getRootLogger().info("Collecting screen image from entire screen.");
				if (image != null && image.exists() && subScreenX != null && subScreenY != null
						&& subScreenWidth != null && subScreenHeight != null) {
					Log.getRootLogger().info(String.format("Collecting screen image from sub screen: %s, %s, %s, %s",
							subScreenX, subScreenY, subScreenWidth, subScreenHeight));
					subImageFile = ImageUtils.getSubImageFromImage(image, Integer.parseInt(String.valueOf(subScreenX)),
							Integer.parseInt(String.valueOf(subScreenY)),
							Integer.parseInt(String.valueOf(subScreenWidth)),
							Integer.parseInt(String.valueOf(subScreenHeight)));
					FileUtils.deleteFile(image);
					image = subImageFile;
				}

				if (image != null && image.exists()) {
					break;
				}
				Log.getRootLogger().warn("Image capture during screen capture failed. Retrying...");
				SleepUtils.sleep(250);
			}
		}

		return image;
	}

}