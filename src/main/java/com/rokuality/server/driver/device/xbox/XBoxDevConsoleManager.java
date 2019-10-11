package com.rokuality.server.driver.device.xbox;

import org.eclipse.jetty.util.log.Log;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.*;
import java.util.concurrent.TimeUnit;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.utils.OSUtils;
import com.rokuality.server.utils.SleepUtils;
import com.rokuality.server.utils.WebDriverUtils;

public class XBoxDevConsoleManager {

	private static final int DEFAULT_TIMEOUT = 60;
	// TODO - clean up the spaghetti exception code below with concurrent exception
	// handling

	private String deviceip = "";

	public XBoxDevConsoleManager(String deviceip) {
		this.deviceip = deviceip;
	}

	public boolean installXBoxApp(String appPackage, String appID) {
        Boolean installSuccess = false;
        WebDriver webDriver = null;
        try {
            webDriver = getWebDriver();

            webDriver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
	
			webDriver.get(getConsoleUrl());
			new WebDriverUtils(webDriver, By.xpath("//div[contains(@class, 'launcherFieldName')]/a[text()='Dev Home']"));
			new WebDriverUtils(webDriver, By.id("wdp-xbox-launcher-deploycommand")).click();
			new WebDriverUtils(webDriver, By.id("xboxdeployment-deploynewapp-packagefile")).type(appPackage);
			new WebDriverUtils(webDriver, By.id("xboxdeployment-nextpage")).click();
			new WebDriverUtils(webDriver, By.id("xboxdeployment-getdependencies-startdeploy")).click();
			new WebDriverUtils(webDriver, By.xpath("//span[text()='Package Successfully Registered']"));
			new WebDriverUtils(webDriver, By.id("xboxdeployment-showinstallationprogress-cancelordone")).click();

			new WebDriverUtils(webDriver, By.xpath("//div[contains(@class, 'launcherFieldName')]/a[text()='" 
					+ appID + "']/../..//div[contains(@class, 'launcherFieldActions')]/select"))
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
			try {
				String pageSource = webDriver.getPageSource();
				pageSource = pageSource.replace(System.lineSeparator(), "");
			} catch (Exception e2) {
				Log.getRootLogger().warn(e2);
			}

			try {
				File screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE);
				// TODO - save failure screenshot in the temp
			} catch (Exception e2) {
				Log.getRootLogger().warn(e2);
			}
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
	
			webDriver.get(getConsoleUrl());
			new WebDriverUtils(webDriver, By.xpath("//div[contains(@class, 'launcherFieldName')]/a[text()='" 
					+ appID + "']/../..//div[contains(@class, 'launcherFieldActions')]/select"))
					.selectByValue("uninstall");

			long pollStart = System.currentTimeMillis();
			long pollMax = pollStart + DEFAULT_TIMEOUT * 1000;

			while (System.currentTimeMillis() < pollMax) {
				webDriver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
				try {
					new WebDriverUtils(webDriver, By.xpath("//div[contains(@class, 'launcherFieldName')]/a[text()='" + appID + "']"));
				} catch (NoSuchElementException nse) {
					uninstallSuccess = true;
					break;
				}
				SleepUtils.sleep(250);
			}
        } catch (Exception e) {
			try {
				String pageSource = webDriver.getPageSource();
				pageSource = pageSource.replace(System.lineSeparator(), "");
			} catch (Exception e2) {
				Log.getRootLogger().warn(e2);
			}

			try {
				File screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE);
				// TODO - save failure screenshot in the temp
			} catch (Exception e2) {
				Log.getRootLogger().warn(e2);
			}
        } finally {
            if (webDriver != null) {
                webDriver.quit();
            }
        }

		return uninstallSuccess;
	}

	private WebDriver getWebDriver() {
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setJavascriptEnabled(true);                
		capabilities.setCapability("takesScreenshot", true);  
		capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, getPhantomJSBin());
		return new PhantomJSDriver(capabilities);
	}

	private File getPhantomJSBin() {
		File bin = null;
		switch (OSUtils.getOSType()) {
			case WINDOWS:
			bin = DependencyConstants.PHANTOM_JS_WINDOWS;
			break;
			case MAC:
			bin = DependencyConstants.PHANTOM_JS_MAC;
			break;
			case LINUX:
			bin = DependencyConstants.PHANTOM_JS_LINUX;
			break;
			default:
			bin = DependencyConstants.PHANTOM_JS_MAC;
			break;
		}
		return bin;
	}

	private String getConsoleUrl() {
		return "https://" + deviceip + ":11443";
	}
}
