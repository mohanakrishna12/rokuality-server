package com.rokuality.server.core;

import org.eclipse.jetty.util.log.Log;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyReader {

	public static final String PROP_PASSWORD_PROP = "proppassword";

	private static Properties properties = null;

	public static String getValue(String key) {
		if (properties == null) {
			Log.getRootLogger().info("Loading configuration properties on startup.", new Object[] {});
			String propPassword = PROP_PASSWORD_PROP;

			StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
			String pass = System.getProperty(propPassword);
			if (pass != null) {
				encryptor.setPassword(pass);
			}

			properties = new EncryptableProperties(encryptor);
			try (InputStream inputStream = PropertyReader.class.getResourceAsStream("server.properties")) {
				properties.load(inputStream);
			} catch (IOException e) {
				Log.getRootLogger().warn("Failed to find configuration property file!", e);
			}
		}

		return properties.getProperty(key);
	}

}
