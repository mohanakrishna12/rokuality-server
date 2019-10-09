package com.rokuality.server.driver.device;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.util.log.Log;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.enums.SessionCapabilities;

public class RokuDevConsoleManager {

	private static final int DEFAULT_TIMEOUT = 60;
	// TODO - clean up the spaghetti exception code below with concurrent exception
	// handling

	private String username = "";
	private String password = "";
	private String deviceip = "";

	public RokuDevConsoleManager(String deviceip, String username, String password) {
		this.username = username;
		this.password = password;
		this.deviceip = deviceip;
	}

	public boolean installRokuApp(String appPackage) {
		String uri = getEndpointUri(deviceip, "plugin_install");
		File appPackageFile = new File(appPackage);

		Log.getRootLogger().info("Installing Roku app package '" + appPackage + "' at console url: " + uri,
				new Object[] {});
		boolean installSuccess = false;
		try {
			HttpHost target = getTarget(deviceip);
			Header digestAuthHeader = getAuthHeader(deviceip, "plugin_install");

			if (digestAuthHeader == null) {
				Log.getRootLogger()
						.warn(String.format(
								"Failed to get authentication header during app install! "
										+ "Is your device connected and listening at the provided %s capability?",
								SessionCapabilities.DEVICE_IP_ADDRESS.value()));
				return false;
			}

			CredentialsProvider credsProvider = getCredsProvider(target, username, password);

			try (CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider)
					.setDefaultRequestConfig(getRequestConfigTimeouts()).build()) {
				AuthCache authCache = new BasicAuthCache();
				DigestScheme digestAuth = new DigestScheme();
				digestAuth.processChallenge(digestAuthHeader);
				authCache.put(target, digestAuth);

				HttpClientContext localContext = HttpClientContext.create();
				localContext.setAuthCache(authCache);

				HttpPost post = new HttpPost(uri);
				HttpEntity entity = MultipartEntityBuilder.create().addTextBody("mysubmit", "Install")
						.addBinaryBody("archive", appPackageFile, ContentType.create("application/zip"),
								appPackageFile.getName())
						.build();

				post.setEntity(entity);

				try (CloseableHttpResponse response = httpclient.execute(target, post, localContext)) {
					int statusCode = response.getStatusLine().getStatusCode();
					installSuccess = statusCode == 200;
					if (!installSuccess) {
						Log.getRootLogger().warn("Roku app install failed with response: " + response.getStatusLine()
								+ System.lineSeparator()
								+ EntityUtils.toString(response.getEntity()).replace(System.lineSeparator(), " "),
								new Object[] {});
					}
				} catch (Exception e) {
					Log.getRootLogger().warn(e);
				}
			} catch (MalformedChallengeException e) {
				Log.getRootLogger().warn(e);
			}
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		return installSuccess;
	}

	public boolean uninstallRokuApp() {
		String uri = getEndpointUri(deviceip, "plugin_install");

		Log.getRootLogger().info("Uninstalling sideloaded Roku app package", new Object[] {});
		boolean uninstallSuccess = false;
		try {
			HttpHost target = getTarget(deviceip);
			Header digestAuthHeader = getAuthHeader(deviceip, "plugin_install");

			if (digestAuthHeader == null) {
				Log.getRootLogger()
						.warn(String.format(
								"Failed to get authentication header during app uninstall! "
										+ "Is your device connected and listening at the provided %s capability?",
								SessionCapabilities.DEVICE_IP_ADDRESS.value()));
				return false;
			}

			CredentialsProvider credsProvider = getCredsProvider(target, username, password);

			try (CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider)
					.setDefaultRequestConfig(getRequestConfigTimeouts()).build()) {
				AuthCache authCache = new BasicAuthCache();
				DigestScheme digestAuth = new DigestScheme();
				digestAuth.processChallenge(digestAuthHeader);
				authCache.put(target, digestAuth);

				HttpClientContext localContext = HttpClientContext.create();
				localContext.setAuthCache(authCache);

				HttpPost post = new HttpPost(uri);
				HttpEntity entity = MultipartEntityBuilder.create().addTextBody("mysubmit", "Delete")
						.addTextBody("archive", " ").build();

				post.setEntity(entity);

				try (CloseableHttpResponse response = httpclient.execute(target, post, localContext)) {
					int statusCode = response.getStatusLine().getStatusCode();
					uninstallSuccess = statusCode == 200;
					if (!uninstallSuccess) {
						Log.getRootLogger().warn("Roku app uninstall failed with response: " + response.getStatusLine()
								+ System.lineSeparator()
								+ EntityUtils.toString(response.getEntity()).replace(System.lineSeparator(), " "),
								new Object[] {});
					}
				} catch (Exception e) {
					Log.getRootLogger().warn(e);
				}
			} catch (MalformedChallengeException e) {
				Log.getRootLogger().warn(e);
			}
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		return uninstallSuccess;
	}

	public File getScreenshot() {
		String uri = getEndpointUri(deviceip, "plugin_inspect");

		Log.getRootLogger().info("Getting screenshot from Roku.", new Object[] {});
		String takeOutput = "";

		Header digestAuthHeader = null;
		CredentialsProvider credsProvider = null;
		HttpHost target = null;
		try {
			target = getTarget(deviceip);
			digestAuthHeader = getAuthHeader(deviceip, "plugin_inspect");
			credsProvider = getCredsProvider(target, username, password);

			try (CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider)
					.setDefaultRequestConfig(getRequestConfigTimeouts()).build()) {
				AuthCache authCache = new BasicAuthCache();
				DigestScheme digestAuth = new DigestScheme();
				digestAuth.processChallenge(digestAuthHeader);
				authCache.put(target, digestAuth);

				HttpClientContext localContext = HttpClientContext.create();
				localContext.setAuthCache(authCache);

				HttpPost post = new HttpPost(uri);
				HttpEntity entity = MultipartEntityBuilder.create().addTextBody("mysubmit", "Screenshot")
						.addTextBody("archive", " ").addTextBody("passwd", " ").build();

				post.setEntity(entity);

				try (CloseableHttpResponse response = httpclient.execute(target, post, localContext)) {
					int statusCode = response.getStatusLine().getStatusCode();
					takeOutput = EntityUtils.toString(response.getEntity());
					boolean takeSuccess = statusCode == 200;
					if (!takeSuccess) {
						Log.getRootLogger().warn(
								"Roku app screenshot take failed with response: " + response.getStatusLine(),
								new Object[] {});
					}
				} catch (Exception e) {
					Log.getRootLogger().warn(e);
				}
			} catch (MalformedChallengeException e) {
				Log.getRootLogger().warn(e);
			}
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		File jpgFile = null;
		try {
			if (takeOutput.toLowerCase().contains("screenshot ok")) {
				try {
					try (CloseableHttpClient httpclient = HttpClients.custom()
							.setDefaultRequestConfig(getRequestConfigTimeouts())
							.setDefaultCredentialsProvider(credsProvider).build()) {
						AuthCache authCache = new BasicAuthCache();
						DigestScheme digestAuth = new DigestScheme();
						digestAuth.processChallenge(digestAuthHeader);
						authCache.put(target, digestAuth);

						HttpClientContext localContext = HttpClientContext.create();
						localContext.setAuthCache(authCache);

						Long currentEpoch = (System.currentTimeMillis() - 1000) / 1000;
						HttpGet get = new HttpGet(
								"http://" + deviceip + "/pkgs/dev.jpg?time=" + currentEpoch.toString());

						try (CloseableHttpResponse response = httpclient.execute(target, get, localContext)) {
							int statusCode = response.getStatusLine().getStatusCode();
							HttpEntity entity = response.getEntity();
							InputStream inputStream = entity.getContent();

							String jpegLocation = DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator
									+ System.currentTimeMillis() + "_" + String.valueOf(deviceip).replace(".", "") + ".jpg";
							jpgFile = new File(jpegLocation);
							FileUtils.copyInputStreamToFile(inputStream, jpgFile);

							if (statusCode != 200) {
								Log.getRootLogger().warn(
										"Roku app screenshot get failed with response: " + response.getStatusLine());
								return null;
							}

							if (jpgFile.exists() && jpgFile.isFile()) {
								return jpgFile;
							}
						} catch (Exception e) {
							Log.getRootLogger().warn(e);
						}
					} catch (MalformedChallengeException e) {
						Log.getRootLogger().warn(e);
					}
				} catch (Exception e) {
					Log.getRootLogger().warn(e);
				}
			} else {
				Log.getRootLogger().warn("Failed to get roku screenshot!");
			}
		} catch (Exception e) {
			Log.getRootLogger().warn("Failed to capture roku screenshot!", e);
		}

		return null;
	}

	private static Header getAuthHeader(String deviceIP, String endpoint) {
		String uri = getEndpointUri(deviceIP, endpoint);

		Header digestAuthHeader = null;

		try (CloseableHttpClient httpClient = HttpClientBuilder.create()
				.setDefaultRequestConfig(getRequestConfigTimeouts()).build()) {
			CloseableHttpResponse response = httpClient.execute(new HttpPost(uri));
			digestAuthHeader = response.getFirstHeader("WWW-Authenticate");
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		return digestAuthHeader;
	}

	private static String getBaseUri(String deviceIP) {
		return "http://" + deviceIP;
	}

	private static String getEndpointUri(String deviceIP, String endpointName) {
		return getBaseUri(deviceIP) + "/" + endpointName;
	}

	private static HttpHost getTarget(String deviceIP) throws MalformedURLException {
		URL url = new URL(getBaseUri(deviceIP));
		return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
	}

	private static CredentialsProvider getCredsProvider(HttpHost target, String username, String password) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(target.getHostName(), target.getPort()),
				new UsernamePasswordCredentials(username, password));
		return credsProvider;
	}

	private static RequestConfig getRequestConfigTimeouts() {
		int timeout = DEFAULT_TIMEOUT;
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
		return config;
	}
}
