package com.rokuality.server.utils;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.util.log.Log;

import com.rokuality.server.constants.DependencyConstants;

import java.io.File;
import java.io.InputStream;

public class HttpUtils {

	private static final Integer CONNECT_TIMEOUT_S = 30;
	private static final Integer DOWNLOAD_TIMEOUT_S = 179;

	public static boolean isValidUrl(String url) {
		String[] schemes = {};
		UrlValidator urlValidator = new UrlValidator(schemes,
				UrlValidator.ALLOW_2_SLASHES + UrlValidator.ALLOW_ALL_SCHEMES);
		return (url != null && urlValidator.isValid(url));
	}

	// TODO cleanup and return null if not properly downloaded/handled
	public static String downloadFile(String fileURL, String fileName) {
		String filePath = DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + fileName;
		File file = new File(filePath);

		Log.getRootLogger().info("Downloading file from '" + fileURL + "' to '" + filePath + "'.", new Object[] {});

		CloseableHttpClient client = null;
		try {
			RequestConfig config = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT_S * 1000)
					.setConnectionRequestTimeout(DOWNLOAD_TIMEOUT_S * 1000).setSocketTimeout(DOWNLOAD_TIMEOUT_S * 1000)
					.build();

			client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();

			HttpGet request = new HttpGet(fileURL);
			HttpResponse response = client.execute(request);
			Log.getRootLogger().info("Result of file download: " + response.getStatusLine(), new Object() {
			});

			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			FileUtils.copyInputStreamToFile(inputStream, file);

			if (file.exists()) {
				Log.getRootLogger().info("File download complete!", new Object[] {});
				Log.getRootLogger().info("File size: " + file.length(), new Object[] {});
			}
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		} finally {
			if (client != null) {
				try {
					client.close();
				} catch (Exception e) {
					Log.getRootLogger().warn(e);
				}
			}
		}

		return filePath;
	}

}
