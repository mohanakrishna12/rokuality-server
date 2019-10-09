package com.rokuality.server.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.eclipse.jetty.util.log.Log;

public class FileUtils {

	private FileUtils() {
		throw new IllegalStateException();
	}

	public static boolean createFile(File fileToCreate) {
		if (fileToCreate == null) {
			return false;
		}

		deleteFile(fileToCreate);
		boolean fileCreated = false;
		try {
			if (isSafeFileAction(fileToCreate)) {
				fileCreated = fileToCreate.createNewFile();
			}
		} catch (IOException e) {
			Log.getRootLogger().warn("Failed to create file.", e);
		}

		return fileCreated;
	}

	public static boolean deleteFile(File fileToDelete) {
		boolean deleted = false;
		if (fileToDelete != null && fileToDelete.isFile() && fileToDelete.exists() && isSafeFileAction(fileToDelete)) {
			try {
				deleted = Files.deleteIfExists(fileToDelete.toPath());
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}

			if (!deleted) {
				Log.getRootLogger().warn("Failed to delete file " + fileToDelete.getAbsolutePath());
			}
		}

		return deleted;
	}

	public static String readStringFromFile(File file) {
		if (file == null) {
			Log.getRootLogger().warn("Unable to read string from null file object!");
			return null;
		}

		if (!file.exists()) {
			Log.getRootLogger()
					.warn("Unable to read string from file " + file.getAbsolutePath() + " which does not exist!");
			return null;
		}

		String fileContent = null;
		try {
			fileContent = org.apache.commons.io.FileUtils.readFileToString(file, "UTF-8");
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		return fileContent;
	}

	public static boolean writeStringToFile(File file, String input, boolean... setExecutable) {
		if (file == null) {
			Log.getRootLogger().warn("Unable to create null file object!");
			return false;
		}

		deleteFile(file);

		boolean fileExists = true;
		if (!file.exists() && isSafeFileAction(file)) {
			try {
				fileExists = file.createNewFile();
			} catch (IOException e) {
				Log.getRootLogger().warn(e);
			}

			if (!fileExists) {
				Log.getRootLogger().warn("Failed to create new file at " + file.getAbsolutePath());
				return false;
			}
		}

		if (file.exists() && setExecutable.length > 0 && setExecutable[0]) {
			boolean setToExecutable = file.setExecutable(setExecutable[0]);
			if (!setToExecutable) {
				Log.getRootLogger().warn("Failed to set file to executable at " + file.getAbsolutePath());
			}
		}

		boolean fileWrittenTo = true;
		try {
			org.apache.commons.io.FileUtils.writeStringToFile(file, input, StandardCharsets.UTF_8);
		} catch (IOException e) {
			fileWrittenTo = false;
			Log.getRootLogger().warn(e);
		}

		if (!fileWrittenTo) {
			Log.getRootLogger().warn("Failed to write input '" + input + "' to file at " + file.getAbsolutePath());
		}

		return fileWrittenTo;
	}

	public static boolean appendStringToFile(File file, String input) {
		if (file == null) {
			Log.getRootLogger().warn("Unable to append to null file object!");
			return false;
		}

		if (!file.exists() || !isSafeFileAction(file)) {
			Log.getRootLogger()
					.warn("Unable to append to file that does not exist or is not safe at " + file.getAbsolutePath());
			return false;
		}

		Path path = Paths.get(file.getAbsolutePath());

		try {
			Files.write(path, input.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			Log.getRootLogger().warn(e);
			return false;
		}

		return true;
	}

	public static boolean copyFile(File fileToCopy, File copiedFile) {
		if (fileToCopy == null || !fileToCopy.exists() || !isSafeFileAction(fileToCopy)) {
			return false;
		}

		boolean fileCopied = false;
		try {
			org.apache.commons.io.FileUtils.copyFile(fileToCopy, copiedFile);
			fileCopied = true;
		} catch (IOException e) {
			Log.getRootLogger().warn(e);
		}

		return fileCopied;
	}

	public static boolean moveFile(File fileToMove, File movedFile) {
		boolean fileCopied = copyFile(fileToMove, movedFile);
		boolean fileDeleted = deleteFile(fileToMove);
		return fileCopied && fileDeleted;
	}

	public static boolean cleanDirectory(File directoryToClean) {
		boolean cleaned = false;
		if (directoryToClean != null && directoryToClean.exists() && directoryToClean.isDirectory()
				&& isSafeFileAction(directoryToClean)) {
			try {
				org.apache.commons.io.FileUtils.cleanDirectory(directoryToClean);
				cleaned = true;
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}

			if (!cleaned) {
				Log.getRootLogger().warn("Failed to clean directory at " + directoryToClean.getAbsolutePath());
			}
		}

		return cleaned;
	}

	public static boolean deleteDirectory(File directoryToDelete) {
		boolean deleted = false;
		if (directoryToDelete != null && directoryToDelete.exists() && directoryToDelete.isDirectory()
				&& isSafeFileAction(directoryToDelete)) {
			try {
				org.apache.commons.io.FileUtils.deleteDirectory(directoryToDelete);
				deleted = true;
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}

			if (!deleted) {
				Log.getRootLogger().warn("Failed to delete directory at " + directoryToDelete.getAbsolutePath());
			}
		}

		return deleted;
	}

	public static boolean copyDirectory(File directoryToCopy, File newDirectory) {
		boolean copied = false;
		if (directoryToCopy != null && newDirectory != null && directoryToCopy.exists() && directoryToCopy.isDirectory()
				&& isSafeFileAction(directoryToCopy) && isSafeFileAction(newDirectory)) {
			try {
				org.apache.commons.io.FileUtils.copyDirectory(directoryToCopy, newDirectory);
				copied = true;
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}

			if (!copied) {
				Log.getRootLogger().warn(
						"Failed to copy directory at " + directoryToCopy.getAbsolutePath() + " to " + newDirectory);
			}
		}

		return copied;
	}

	public static boolean moveDirectory(File directoryToMove, File movedDirectory) {
		boolean copiedDir = copyDirectory(directoryToMove, movedDirectory);
		boolean dirDeleted = deleteDirectory(directoryToMove);
		return copiedDir && dirDeleted;
	}

	public static boolean createDirectory(File directoryToCreate) {
		boolean success = false;
		try {
			if (!directoryToCreate.exists() && isSafeFileAction(directoryToCreate)) {
				success = directoryToCreate.mkdirs();
			} else {
				Log.getRootLogger().info(
						"Directory already exists or is not safe at " + directoryToCreate.getAbsolutePath(),
						new Object[] {});
			}
		} catch (SecurityException e) {
			Log.getRootLogger().warn("Failed to create directory at " + directoryToCreate.getAbsolutePath(), e);
		}

		return success;
	}

	public static boolean isSafeFileAction(File file) {
		return file.getAbsolutePath().startsWith(OSUtils.getUserBaseDir().getAbsolutePath());
	}

}
