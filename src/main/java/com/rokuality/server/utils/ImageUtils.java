package com.rokuality.server.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jetty.util.log.Log;
import org.imgscalr.Scalr;
import org.json.simple.JSONObject;
import org.sikuli.script.Finder;
import org.sikuli.script.Image;
import org.sikuli.script.Match;
import org.sikuli.script.Pattern;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.protobuf.ByteString;
import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.constants.TesseractConstants;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.core.ocr.ImageText;
import com.rokuality.server.driver.device.roku.RokuDevConsoleManager;
import com.rokuality.server.driver.device.xbox.XBoxDevAPIManager;
import com.rokuality.server.enums.OCRType;
import com.rokuality.server.enums.PlatformType;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ImageUtils {

	private static final Integer MIN_FILE_SIZE_B = 100;
	private static final int MAX_IMAGE_CAPTURE_ATTEMPTS = 3;

	public static File getImageFromDevice(String sessionID) {
		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);
		PlatformType platform = PlatformType
				.getEnumByString(String.valueOf(sessionInfo.get(SessionConstants.PLATFORM)));

		File image = null;

		for (int i = 1; i <= MAX_IMAGE_CAPTURE_ATTEMPTS; i++) {
			if (i != 0) {
				Log.getRootLogger().warn("Attempting " + String.valueOf(platform) + " screen capture after failed attempt.", i);
			}

			switch (platform) {
			case ROKU:
				image = SessionManager.getImageCollector(sessionID).getCurrentImage(false);
				break;
			case XBOX:

				break;
			case CHROMECAST:

				break;
			case PLAYSTATION:

				break;
			default:
				image = SessionManager.getImageCollector(sessionID).getCurrentImage(false);
				break;
			}

			File resizedImageFile = null;
			if (image != null && image.exists() && image.length() >= MIN_FILE_SIZE_B) {
				JSONObject sessionInfoObj = SessionManager.getSessionInfo(sessionID);
				if (sessionInfoObj == null) {
					return null;
				}

				Object resizeObj = sessionInfoObj.get(SessionConstants.SCREEN_SIZE_OVERRIDE);
				String defaultResize = String.valueOf(resizeObj);
				if (!defaultResize.equals("null")) {
					Log.getRootLogger().info(
							"Resizing image '" + image.getAbsolutePath() + "' to size '" + defaultResize + "'.");

					String[] size = null;
					try {
						size = defaultResize.toLowerCase().replace(" ", "").split("x");
					} catch (Exception e) {
						// TODO - custom exception
						throw new RuntimeException("The default image sizing is not in the proper 'width,height' "
								+ "format. You provided '" + defaultResize
								+ "'. A working example would be '2400,1800'");
					}

					// TODO - number format exception handling!
					resizedImageFile = ImageUtils.resizeImage(image, Integer.parseInt(size[0]),
							Integer.parseInt(size[1]));
					if (resizedImageFile != null && resizedImageFile.exists()) {
						com.rokuality.server.utils.FileUtils.deleteFile(image);
						image = resizedImageFile;
						return image;
					}
				}

				if (image != null && image.exists()) {
					return image;
				}
			}

			SleepUtils.sleep(250);
		}

		return null;
	}

	public static JSONObject getElementFromSubScreen(String sessionID, String locator, Long subScreenX, Long subScreenY,
			Long subScreenWidth, Long subScreenHeight) {
		File screenImage = getImageFromDevice(sessionID);
		if (screenImage == null || !screenImage.exists()) {
			return null;
		}
		File subScreenImage = getSubImageFromImage(screenImage, subScreenX.intValue(), subScreenY.intValue(),
				subScreenWidth.intValue(), subScreenHeight.intValue());
		com.rokuality.server.utils.FileUtils.deleteFile(screenImage);
		return getElement(sessionID, locator, subScreenImage);
	}

	public static JSONObject getElementFromEntireScreen(String sessionID, String locator) {
		File screenImage = getImageFromDevice(sessionID);
		if (screenImage == null || !screenImage.exists()) {
			return null;
		}
		return getElement(sessionID, locator, screenImage);
	}

	private static JSONObject getElement(String sessionID, String locator, File screenImage) {
		JSONObject element = new JSONObject();
		element.put(SessionConstants.ELEMENT_ID, UUID.randomUUID().toString());

		boolean isFile = locator != null && new File(locator).exists();
		boolean elementFound = false;

		if (!screenImage.exists()) {
			return null;
		}

		if (isFile) {
			Finder finder = null;
			float similarity = Float.valueOf(
					SessionManager.getSessionInfo(sessionID).get(SessionConstants.IMAGE_MATCH_SIMILARITY).toString());
			Pattern pattern = null;

			try {
				pattern = new Pattern(locator).similar(similarity);
				finder = new Finder(screenImage.getAbsolutePath());
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}

			if (finder != null && pattern != null) {
				finder.find(pattern);
			}

			if (finder != null && finder.hasNext()) {
				Match match = finder.next();
				int x = match.x;
				int y = match.y;
				double width = Double.valueOf(match.w);
				double height = Double.valueOf(match.h);
				double confidence = match.getScore();
				String text = getTextFromImage(SessionManager.getOCRType(sessionID), new File(locator),
						SessionManager.getGoogleCredentials(sessionID));

				element.put("element_x", x);
				element.put("element_y", y);
				element.put("element_width", width);
				element.put("element_height", height);
				element.put("element_confidence", confidence);
				element.put("element_text", text);
				elementFound = true;
			}
		}

		if (!isFile) {
			Log.getRootLogger().info("Finder is text " + locator, sessionID);

			OCRType ocrType = SessionManager.getOCRType(sessionID);
			List<ImageText> imageTexts = getTextsListFromImage(ocrType, screenImage,
					SessionManager.getGoogleCredentials(sessionID));
			ImageText imageText = getConstructedTextElement(ocrType, imageTexts, locator);
			if (imageText != null) {
				element.put("element_x", imageText.getLocation().x);
				element.put("element_y", imageText.getLocation().y);
				element.put("element_width", imageText.getLength());
				element.put("element_height", imageText.getWidth());
				element.put("element_confidence", imageText.getConfidence());
				element.put("element_text", imageText.getText());
				elementFound = true;
			}
		}

		com.rokuality.server.utils.FileUtils.deleteFile(screenImage);

		if (!elementFound) {
			return null;
		}
		return element;
	}

	public static ImageText getConstructedTextElement(OCRType ocrType, List<ImageText> imageTexts, String locator) {
		if (imageTexts == null || imageTexts.isEmpty() || locator == null || locator.isEmpty()) {
			Log.getRootLogger().warn("ImageText/Locator are invalid during text element construction!");
			return null;
		}

		// remove the first entry which is an entire page word match
		if (ocrType.equals(OCRType.GOOGLE_VISION) && imageTexts.size() > 1) {
			imageTexts.remove(0);
		}

		// find all components of the locator test
		List<String> loc = new ArrayList<String>(); // TODO - case sensitive based on user capability
		loc.add(locator.trim().toLowerCase());
		if (locator.contains(" ")) {
			loc = Arrays.asList(locator.trim().toLowerCase().split(" "));
		}

		// get all the matching ImageText objects for our locator
		List<ImageText> matchedImageTexts = new ArrayList<>();
		for (ImageText imageText : imageTexts) {
			String imageTxt = imageText.getText().trim().toLowerCase(); // TODO - case sensitive based on user
																		// capability
			if (loc.contains(imageTxt) && matchedImageTexts.size() < loc.size()) { // SHOULD PREVENT MULTIPLE WORD
																					// MATCHES FROM BEING APPENDED
				matchedImageTexts.add(imageText);
			}
		}

		List<Integer> yLocElements = new ArrayList<Integer>();
		for (ImageText imageText : matchedImageTexts) {
			yLocElements.add(imageText.getLocation().y);
		}

		// TODO - implement logic for something like below to remove words tha that are
		// multiple hits
		// but exist on different lines, i.e a locator of "are you sure" for a page with
		// text "are you sure you"
		// doesnt return multiple hits if one of the matched words is on a different
		// line.
		if (yLocElements.size() > 2) {
			yLocElements = OutlierUtils.eliminateOutliers(yLocElements, 1.5f);
		}

		String constructedWords = "";
		for (ImageText imageText : matchedImageTexts) {
			constructedWords += " " + imageText.getText();
		}
		constructedWords = constructedWords.trim();

		if (matchedImageTexts.isEmpty()) {
			return null;
		}

		// if locator is multi world string "Hello World!", ensure the count of
		// matched words equals the count of the locator word string
		if (loc.size() > 1 && loc.size() != matchedImageTexts.size()) {
			return null;
		}

		if (matchedImageTexts.size() == 1) {
			return matchedImageTexts.get(0);
		}

		ImageText constructedImageText = new ImageText();
		constructedImageText.setText(constructedWords);

		// check if the fully constructed locator matches the constructed image image texts
		// so if locator is "World! Hello", a match won't be found for "Hello World!";
		if (!locator.trim().toLowerCase().equals(constructedWords.trim().toLowerCase())) {
			return null;
		}

		int startX = matchedImageTexts.get(0).getLocation().x;
		int startY = matchedImageTexts.get(0).getLocation().y;
		int endX = (int) matchedImageTexts.get(matchedImageTexts.size() - 1).getLength();
		int width = startX + endX; // x-axis 'width'
		int height = (int) matchedImageTexts.get(0).getWidth(); // y-axis 'height'

		constructedImageText.setLength(Double.valueOf((width)));
		constructedImageText.setWidth(Double.valueOf(height));
		constructedImageText.setLocation(new Point(startX, startY));

		// TODO - performan a median evaluate of all confidences and return that
		// for multiple word matches
		constructedImageText.setConfidence(matchedImageTexts.get(0).getConfidence());

		return constructedImageText;
	}

	public static String imageHandler(String locatorSource) {
		if (HttpUtils.isValidUrl(locatorSource)) {
			// locator is a url and we need to download the file
			try {
				File imageFile = new File(HttpUtils.downloadFile(locatorSource, UUID.randomUUID().toString()));
				String formatName = getImageFormat(imageFile);

				File readyFile = new File(imageFile.getAbsolutePath() + formatName);
				FileUtils.moveFile(imageFile, readyFile);

				return readyFile.getAbsolutePath();
			} catch (Exception e) {
				Log.getRootLogger().warn("Failed to download/save image locator from '" + locatorSource + "'.", e);
				return null;
			}
		} else {
			// locator is a base64 string sent from that we need to convert
			byte[] fileData = null;
			try {
				fileData = Base64.getDecoder().decode(locatorSource);
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
				return null;
			}

			File imageFile = new File(DependencyConstants.TEMP_DIR.getAbsoluteFile() + File.separator + UUID.randomUUID().toString());
			boolean fileCreated = com.rokuality.server.utils.FileUtils.createFile(imageFile);
			if (!fileCreated) {
				Log.getRootLogger().warn("Failed to create file from base64 image handler!");
				return null;
			}

			try (FileOutputStream fileOutputStream = new FileOutputStream(imageFile)) {
				fileOutputStream.write(fileData);		
			} catch (Exception e) {
				Log.getRootLogger().warn("Failed to decode image file for execution.", e);
				return null;
			}
			
			String formatName = getImageFormat(imageFile);
			if (formatName == null) {
				return null;
			}
			File readyFile = new File(imageFile.getAbsolutePath() + formatName);
			boolean fileMoved = com.rokuality.server.utils.FileUtils.moveFile(imageFile, readyFile);
			if (fileMoved) {
				return readyFile.getAbsolutePath();
			}
		}

		return null;
	}

	public static File resizeImage(File imageToResize, Integer width, Integer height) {
		File resizedFile = null;
		try {
			resizedFile = new File(imageToResize.getParent() + File.separator + "resized_"
					+ UUID.randomUUID().toString() + "_" + imageToResize.getName());
			resizedFile.createNewFile();

			String formatName = FilenameUtils.getExtension(imageToResize.getName());

			BufferedImage inputImage = ImageIO.read(imageToResize);
			BufferedImage outputImage = Scalr.resize(inputImage, Scalr.Mode.AUTOMATIC, width, height);
			ImageIO.write(outputImage, formatName, resizedFile);
		} catch (Exception e) {
			Log.getRootLogger().warn("Failed to resize image '" + imageToResize.getAbsolutePath()
					+ "' to size width = '" + width.toString() + "' and height = '" + height + "'.", e);
		}

		return resizedFile;
	}

	public static String getImageFormat(File imageFile) {
		if (imageFile == null || !imageFile.exists()) {
			Log.getRootLogger().warn("Image file is invaild during image format check");
			return null;
		}

		String formatName = "png"; // DEFAULT
		try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(imageFile)) {
			Iterator<ImageReader> iterator = ImageIO.getImageReaders(imageInputStream);
			ImageReader reader = iterator.next();
			formatName = reader.getFormatName();
		} catch (Exception e) {
			Log.getRootLogger().warn("Failed to determine image format. Defaulting to png.", e);
			Log.getRootLogger().warn(e);
		}

		return "." + formatName;
	}

	public static String getTextFromImage(OCRType ocrType, File imageFile, GoogleCredentials credentials) {
		String text = "";
		if (ocrType.equals(OCRType.TESSERACT)) {
			text = getTextFromImageUsingTesseract(imageFile);
		}

		if (ocrType.equals(OCRType.GOOGLE_VISION) && credentials != null) {
			text = getTextFromImageUsingGoogleVision(credentials, imageFile);
		}

		return text;
	}

	private static String getTextFromImageUsingTesseract(File imageFile) {
		Tesseract tesseract = getTesseract();
		String text = null;
		try {
			text = tesseract.doOCR(imageFile);
		} catch (TesseractException te) {
			Log.getRootLogger().warn(te);
		}

		return text;
	}

	private static String getTextFromImageUsingGoogleVision(GoogleCredentials credentials, File imageFile) {
		String text = "";
		ImageAnnotatorClient vision = null;
		try {
			ImageAnnotatorSettings imageAnnotatorSettings = ImageAnnotatorSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();

			vision = ImageAnnotatorClient.create(imageAnnotatorSettings);

			Path path = Paths.get(imageFile.getAbsolutePath());
			byte[] data = Files.readAllBytes(path);
			ByteString imgBytes = ByteString.copyFrom(data);

			List<AnnotateImageRequest> requests = new ArrayList<>();
			com.google.cloud.vision.v1.Image img = com.google.cloud.vision.v1.Image.newBuilder().setContent(imgBytes)
					.build();
			Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
			AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
			requests.add(request);

			BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					Log.getRootLogger().warn("Google vision error: " + res.getError().getMessage());
					break;
				}

				for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
					text += " " + annotation.getDescription();
				}
			}

		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		} finally {
			if (vision != null) {
				try {
					vision.shutdown();
				} catch (Exception e2) {
					Log.getRootLogger().warn(e2);
				}
			}
		}

		return text;
	}

	public static List<ImageText> getTextsListFromImage(OCRType ocrType, File imageFile,
			GoogleCredentials credentials) {
		List<ImageText> imageTexts = new ArrayList<>();
		if (ocrType.equals(OCRType.GOOGLE_VISION)) {
			imageTexts = getTextsListFromImageUsingGoogleVision(credentials, imageFile);
		}

		if (ocrType.equals(OCRType.TESSERACT)) {
			imageTexts = getTextsListFromImageUsingTesseract(imageFile);
		}
		return imageTexts;
	}

	private static List<ImageText> getTextsListFromImageUsingTesseract(File imageFile) {
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = getBufferedImage(imageFile);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		List<ImageText> imageTexts = new ArrayList<>();
		if (bufferedImage == null) {
			return imageTexts;
		}

		try {
			Tesseract tesseract = getTesseract();
			List<Word> words = tesseract.getWords(bufferedImage, ITessAPI.TessPageIteratorLevel.RIL_WORD);
			imageTexts = fillTesseractTexts(words);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		return imageTexts;
	}

	private static List<ImageText> getTextsListFromImageUsingGoogleVision(GoogleCredentials credentials,
			File imageFile) {
		List<ImageText> imageTexts = new ArrayList<>();
		ImageAnnotatorClient vision = null;
		try {
			ImageAnnotatorSettings imageAnnotatorSettings = ImageAnnotatorSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();

			vision = ImageAnnotatorClient.create(imageAnnotatorSettings);

			Path path = Paths.get(imageFile.getAbsolutePath());
			byte[] data = Files.readAllBytes(path);
			ByteString imgBytes = ByteString.copyFrom(data);

			List<AnnotateImageRequest> requests = new ArrayList<>();
			com.google.cloud.vision.v1.Image img = com.google.cloud.vision.v1.Image.newBuilder().setContent(imgBytes)
					.build();
			Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
			AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
			requests.add(request);

			BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
			List<AnnotateImageResponse> responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					Log.getRootLogger().warn("Google vision error: " + res.getError().getMessage());
					break;
				}
				imageTexts = fillGoogleVisionTexts(res);
			}

		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		} finally {
			if (vision != null) {
				try {
					vision.shutdown();
				} catch (Exception e2) {
					Log.getRootLogger().warn(e2);
				}
			}
		}
		return imageTexts;
	}

	private static List<ImageText> fillGoogleVisionTexts(AnnotateImageResponse res) {
		List<ImageText> imageTexts = new ArrayList<>();
		for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
			ImageText imageText = new ImageText();
			int x = annotation.getBoundingPoly().getVerticesList().get(0).getX();
			int y = annotation.getBoundingPoly().getVerticesList().get(0).getY();
			int length = annotation.getBoundingPoly().getVerticesList().get(1).getX()
					- annotation.getBoundingPoly().getVerticesList().get(0).getX();
			int width = annotation.getBoundingPoly().getVerticesList().get(2).getY()
					- annotation.getBoundingPoly().getVerticesList().get(0).getY();
			float confidence = annotation.getScore();
			imageText.setText(annotation.getDescription());
			imageText.setLocation(new Point(x, y));
			imageText.setLength(length);
			imageText.setWidth(width);
			imageText.setConfidence(confidence);
			imageTexts.add(imageText);
		}
		return imageTexts;
	}

	private static List<ImageText> fillTesseractTexts(List<Word> words) {
		List<ImageText> imageTexts = new ArrayList<>();
		for (Word word : words) {
			ImageText imageText = new ImageText();
			Rectangle rectangle = word.getBoundingBox();
			double x = rectangle.getMinX();
			double y = rectangle.getMinY();
			double width = rectangle.getMaxX() - x;
			float confidence = word.getConfidence();
			double height = rectangle.getMaxY() - y;

			imageText.setText(word.getText());
			imageText.setLocation(new Point((int) x, (int) y));
			imageText.setLength(width); // actually the x-axis 'width' of the word
			imageText.setWidth(height); // actually the y-axis 'height' of the word
			imageText.setConfidence(confidence);
			imageTexts.add(imageText);
		}
		return imageTexts;
	}

	public static File getSubImageFromImage(File image, int subX, int subY, int width, int height) {
		File subImage = new File(Image.create(image.getAbsolutePath()).getSub(subX, subY, width, height).asFile());
		return subImage;
	}

	public static BufferedImage getBufferedImage(File file) throws IOException {
		return ImageIO.read(file);
	}

	public static File getScreenImage(PlatformType platform, String username, String password, String deviceip) {
		switch (platform) {
			case ROKU:
			return new RokuDevConsoleManager(deviceip, username, password).getScreenshot();
			case XBOX:
			return new XBoxDevAPIManager(deviceip).getScreenshot();
			default:
			return new RokuDevConsoleManager(deviceip, username, password).getScreenshot();
		}
	}

	private static Tesseract getTesseract() {
		Tesseract tesseract = new Tesseract();
		File tessLog = LogFileUtils.getLogFile("tesseract.log");
		tesseract.setTessVariable("debug_file", tessLog.getAbsolutePath());
		tesseract.setDatapath(TesseractConstants.TESSERACT_TESS_DATA_DIR.getAbsolutePath());
		return tesseract;
	}

}
