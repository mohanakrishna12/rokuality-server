package com.rokuality.server.utils;

import org.eclipse.jetty.util.log.Log;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class WebDriverUtils {

	private WebDriver webDriver = null;
	private By locator = null;
	private WebElement element = null;

	public WebDriverUtils(WebDriver webDriver, By locator) {
		this.webDriver = webDriver;
		this.locator = locator;
		this.element = webDriver.findElement(locator);
	}

	public void click() {
		staleHandler().click();
	}

	public void selectByValue(String value) {
		for (int i = 0; i <= 10; i++) {
			try {
				new Select(staleHandler())
					.selectByValue(value);
				break;
			} catch (StaleElementReferenceException e) {
				try {
					element = webDriver.findElement(locator);
				} catch (Exception e2) {
					Log.getRootLogger().warn(e2);
				}
			}
			SleepUtils.sleep(1000);
		}
	}

	public void type(String text) {
		staleHandler().sendKeys(text);
	}

	private WebElement staleHandler() {
		for (int i = 0; i <= 10; i++) {
			try {
				element.isEnabled();
				break;
			} catch (StaleElementReferenceException e) {
				try {
					element = webDriver.findElement(locator);
				} catch (Exception e2) {
					Log.getRootLogger().warn(e2);
				}
			}
			SleepUtils.sleep(1000);
		}
		SleepUtils.sleep(250);
		return element;
	}

}
