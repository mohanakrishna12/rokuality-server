package com.rokuality.server.utils;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.eclipse.jetty.util.log.Log;

public class FileToStringUtils {

	public String convertToString(File file) {
		if (file.exists() && file.isFile()) {
			byte[] fileData = null;
			Path filePath = Paths.get(file.getAbsolutePath());
			try {
				fileData = Files.readAllBytes(filePath);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (fileData != null) {
				return Base64.getEncoder().encodeToString(fileData);
			}
		}

		return null;
	}

	public File convertToFile(String source, File file) {
		byte[] fileData = Base64.getDecoder().decode(source);
		if (!FileUtils.isSafeFileAction(file)) {
			Log.getRootLogger().warn("Not converting base64 to file at " + file.getAbsolutePath()
					+ " as it is NOT within the temp dir!");
			return null;
		}

		boolean fileCreated = FileUtils.createFile(file);
		if (fileCreated) {
			try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
				fileOutputStream.write(fileData);
			} catch (Exception e) {
				Log.getRootLogger().warn("Failed to decode file for execution.", e);
			}
		}

		return file;
	}

	public String convertFromBaseStringToString(String base64Source) {
		byte[] data = Base64.getDecoder().decode(base64Source);
		return new String(data);
	}

}
