package com.rokuality.server.driver.device.xbox;

import org.eclipse.jetty.util.log.Log;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.*;
import java.util.concurrent.TimeUnit;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.utils.SleepUtils;
import com.rokuality.server.utils.WebDriverUtils;

public class XBoxDevConsoleManager {

	private static final int DEFAULT_TIMEOUT = 30;
	// TODO - clean up the spaghetti exception code below with concurrent exception
	// handling

	private String deviceip = "";

	public XBoxDevConsoleManager(String deviceip) {
		this.deviceip = deviceip;
	}

	public boolean installApp(String appPackage, String appID) {
		Boolean installSuccess = false;
		WebDriver webDriver = null;
		try {
			webDriver = getWebDriver();

			webDriver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);

			webDriver.get(getConsoleUrl());
			new WebDriverUtils(webDriver,
					By.xpath("//div[contains(@class, 'launcherFieldName')]/a[text()='Dev Home']"));
			new WebDriverUtils(webDriver, By.id("wdp-xbox-launcher-deploycommand")).click();
			new WebDriverUtils(webDriver, By.id("xboxdeployment-deploynewapp-packagefile")).type(appPackage);
			new WebDriverUtils(webDriver, By.id("xboxdeployment-nextpage")).click();
			new WebDriverUtils(webDriver, By.id("xboxdeployment-getdependencies-startdeploy")).click();
			new WebDriverUtils(webDriver, By.xpath("//span[text()='Package Successfully Registered']"));
			new WebDriverUtils(webDriver, By.id("xboxdeployment-showinstallationprogress-cancelordone")).click();

			new WebDriverUtils(webDriver,
					By.xpath("//div[contains(@class, 'launcherFieldName')]/a[text()='" + appID
							+ "']/../..//div[contains(@class, 'launcherFieldActions')]/select"))
									.selectByValue("activate");

			long pollStart = System.currentTimeMillis();
			long pollMax = pollStart + DEFAULT_TIMEOUT * 1000;

			while (System.currentTimeMillis() < pollMax) {
				webDriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
				try {
					new WebDriverUtils(webDriver, By.xpath("//div[contains(@class, 'launcherFieldName')]/a[text()='"
							+ appID + "']/../..//div[contains(@class, 'launcherFieldState')][text()='Running']"));
					installSuccess = true;
					break;
				} catch (NoSuchElementException nse) {
					// ignore
				}
				SleepUtils.sleep(250);
			}
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
			getFailureArtifacts(webDriver);
		} finally {
			if (webDriver != null) {
				webDriver.quit();
			}
		}

		return installSuccess;
	}

	public boolean uninstallApp(String appID) {
		boolean uninstallSuccess = false;
		WebDriver webDriver = null;
		try {
			webDriver = getWebDriver();

			webDriver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
			Log.getRootLogger().info(String.format("Uninstalling XBox App %s from device at %s", appID, getConsoleUrl()));
			webDriver.get(getConsoleUrl());
			new WebDriverUtils(webDriver,
					By.xpath("//div[contains(@class, 'launcherFieldName')]/a[text()='Dev Home']"));

			boolean appInstalled = false;
			try {
				webDriver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
				new WebDriverUtils(webDriver,
						By.xpath("//div[contains(@class, 'launcherFieldName')]/a[text()='" + appID
						+ "']/../..//div[contains(@class, 'launcherFieldActions')]/select"))
								.selectByValue("uninstall");
				appInstalled = true;
			} catch (NoSuchElementException nse) {
				Log.getRootLogger().warn(String.format("XBox app with id %s is not installed!", appID));
			}
			
			long pollStart = System.currentTimeMillis();
			long pollMax = pollStart + DEFAULT_TIMEOUT * 1000;

			while (appInstalled && System.currentTimeMillis() < pollMax) {
				webDriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
				try {
					new WebDriverUtils(webDriver,
							By.xpath("//div[contains(@class, 'launcherFieldName')]/a[text()='" + appID + "']"));
				} catch (NoSuchElementException nse) {
					uninstallSuccess = true;
					break;
				}
				SleepUtils.sleep(250);
			}
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
			getFailureArtifacts(webDriver);
		} finally {
			if (webDriver != null) {
				webDriver.quit();
			}
		}

		return uninstallSuccess;
	}

	private WebDriver getWebDriver() {
		DesiredCapabilities capabilities = new DesiredCapabilities();
		io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();

		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("headless");
		chromeOptions.addArguments("window-size=1200x600");
		chromeOptions.addArguments("--ignore-certificate-errors");
		capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
		WebDriver webDriver = new ChromeDriver(capabilities);
		webDriver.manage().window().maximize();
		return webDriver;
	}

	private String getConsoleUrl() {
		return "https://" + deviceip + ":11443";
	}

	private void getFailureArtifacts(WebDriver webDriver) {
		try {
			String pageSource = webDriver.getPageSource();
			pageSource = pageSource.replace(System.lineSeparator(), "");
			Log.getRootLogger().warn(pageSource);
		} catch (Exception e2) {
			Log.getRootLogger().warn(e2);
		}

		try {
			File screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE);
			File tempFailure = new File(DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + "installfail_"
					+ deviceip.replace(".", "") + "_" + System.currentTimeMillis());
			screenshot.renameTo(tempFailure);
			Log.getRootLogger().info("XBox install failure screenshot saved to: " + tempFailure.getAbsolutePath());
		} catch (Exception e2) {
			Log.getRootLogger().warn(e2);
		}
	}
}
