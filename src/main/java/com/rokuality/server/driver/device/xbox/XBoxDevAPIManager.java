package com.rokuality.server.driver.device.xbox;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.util.log.Log;

import java.io.*;

import com.rokuality.server.constants.DependencyConstants;

public class XBoxDevAPIManager {

	private static final int DEFAULT_TIMEOUT = 60;

	private String deviceip = null;
	
	public XBoxDevAPIManager(String deviceip) {
		this.deviceip = deviceip;
	}

	public File getScreenshot() {
		File pngFile = null;
		try (CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultRequestConfig(getRequestConfigTimeouts())
					.build()) {

				String url = "https://" + deviceip + ":11443/ext/screenshot?download=true";
				HttpGet get = new HttpGet(url);

				try (CloseableHttpResponse response = httpclient.execute(get)) {
					int statusCode = response.getStatusLine().getStatusCode();
					HttpEntity entity = response.getEntity();
					InputStream inputStream = entity.getContent();

					String pngLocation = DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator
							+ System.currentTimeMillis() + "_" + String.valueOf(deviceip).replace(".", "") + ".png";
					pngFile = new File(pngLocation);
					FileUtils.copyInputStreamToFile(inputStream, pngFile);

					if (statusCode != 200) {
						Log.getRootLogger().warn(
								"XBox app screenshot get failed with response: " + response.getStatusLine());
						return null;
					}

					if (pngFile.exists() && pngFile.isFile()) {
						return pngFile;
					}
				} catch (Exception e) {
					Log.getRootLogger().warn(e);
				}
			
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		return pngFile;
	}

	private static RequestConfig getRequestConfigTimeouts() {
		int timeout = DEFAULT_TIMEOUT;
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
		return config;
	}

}
