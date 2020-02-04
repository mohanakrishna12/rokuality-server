package com.rokuality.server.driver.device.xbox;

import java.util.concurrent.TimeUnit;

import com.rokuality.server.driver.host.APIManager;
import com.rokuality.server.enums.APIType;
import com.rokuality.server.enums.XBoxButton;
import com.rokuality.server.utils.WebDriverUtils;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

public class XBoxSmartglassAPIManager {

	private String deviceID = "";
	private String username = "";
	private String password = "";
	private int responseCode = -1;
	private JSONObject responseObj = new JSONObject();

	public XBoxSmartglassAPIManager(String deviceID, String username, String password) {
		this.deviceID = deviceID;
		this.username = username;
		this.password = password;
	}

	public boolean authenticate() {
		boolean authSuccess = false;
		WebDriver webDriver = null;
		try {
			XBoxDevConsoleManager devConsoleManager = new XBoxDevConsoleManager(null);
			webDriver = devConsoleManager.getWebDriver();
			webDriver.manage().timeouts().implicitlyWait(XBoxDevConsoleManager.DEFAULT_TIMEOUT, TimeUnit.SECONDS);
			webDriver.get("http://localhost:" + XBoxSmartglassFactory.PORT + "/auth/login");
			new WebDriverUtils(webDriver, By.xpath("//div[@class='loginbox']"));

			try {
				webDriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
				new WebDriverUtils(webDriver, By.xpath("//h3[text()='Already signed in']"));
				Log.getRootLogger().info(String.format("XBox device %s is already authenticated.", deviceID));
				return true;
			} catch (NoSuchElementException nse) {
				// not authenticated
			}

			Log.getRootLogger()
					.info(String.format("XBox device %s is not authenticated. Authenticationg...", deviceID));
			webDriver.manage().timeouts().implicitlyWait(XBoxDevConsoleManager.DEFAULT_TIMEOUT, TimeUnit.SECONDS);

			new WebDriverUtils(webDriver, By.xpath("//input[@name='email']")).type(username);
			new WebDriverUtils(webDriver, By.xpath("//input[@name='password']")).type(password);
			new WebDriverUtils(webDriver, By.xpath("//input[@value='Login']")).click();
			new WebDriverUtils(webDriver, By.xpath("//h3[text()='Login succeeded']"));
			authSuccess = true;
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		} finally {
			if (webDriver != null) {
				webDriver.quit();
			}
		}

		return authSuccess;
	}

	public void connect() {
		sendCommand("GET", "/device");
		sendCommand("GET", "/device/" + deviceID + "/connect");
	}

	public boolean isConnected() {
		sendCommand("GET", "/device/" + deviceID);
		if (this.getResponseCode() != 200) {
			return false;
		}
		
		JSONObject conObj = this.getResponseObj();
		return conObj != null && conObj.toJSONString().contains("Connected");
	}

	public void sendInput(XBoxButton button) {
		sendCommand("GET", "/device/" + deviceID + XBoxButton.getSmartglassInput(button));
	}

	private void sendCommand(String method, String path) {
		APIManager xboxAPIManager = new APIManager(APIType.XBOX_SMARTGLASS, null, path, method);
		xboxAPIManager.sendDevAPICommand();
		responseCode = xboxAPIManager.getResponseCode();
		try {
			responseObj = (JSONObject) new JSONParser().parse(xboxAPIManager.getResponseContent());
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}
	}

	public int getResponseCode() {
		return responseCode;
	}

	public JSONObject getResponseObj() {
		return responseObj;
	}

	public int getXBoxServerResponseCode() {
		if (responseObj != null && responseObj.containsKey("status")) {
			return (int) (long) responseObj.get("status");
		}
		return -1;
	}

}
