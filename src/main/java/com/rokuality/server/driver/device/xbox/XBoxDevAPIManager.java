package com.rokuality.server.driver.device.xbox;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.eclipse.jetty.util.log.Log;

import java.io.*;

import javax.net.ssl.SSLContext;

public class XBoxDevAPIManager {

	private static final int DEFAULT_TIMEOUT = 60;

	private String deviceip = null;
	
	public XBoxDevAPIManager(String deviceip) {
		this.deviceip = deviceip;
	}

	public File getScreenshot(File fileToSaveAs) {
		File pngFile = null;

		SSLConnectionSocketFactory sslSocketFactory = null;
		try {
			SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
			sslContextBuilder.loadTrustMaterial(new org.apache.http.conn.ssl.TrustSelfSignedStrategy());
			SSLContext sslContext = sslContextBuilder.build();
			sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new org.apache.http.conn.ssl.DefaultHostnameVerifier());
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}
		
		if (sslSocketFactory == null) {
			return null;
		}

		try (CloseableHttpClient httpclient = HttpClients.custom()
					.setSSLSocketFactory(sslSocketFactory)
					.setDefaultRequestConfig(getRequestConfigTimeouts())
					.build()) {

				String url = "https://" + deviceip + ":11443/ext/screenshot?download=true";
				HttpGet get = new HttpGet(url);

				try (CloseableHttpResponse response = httpclient.execute(get)) {
					int statusCode = response.getStatusLine().getStatusCode();
					HttpEntity entity = response.getEntity();
					InputStream inputStream = entity.getContent();

					pngFile = fileToSaveAs;
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
