package com.rokuality.server.driver.host;

import org.eclipse.jetty.util.log.Log;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.FFMPEGConstants;
import com.rokuality.server.constants.RokuWebDriverConstants;
import com.rokuality.server.constants.TesseractConstants;
import com.rokuality.server.core.CommandExecutor;
import com.rokuality.server.core.DependencyManager;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.LogFileUtils;
import com.rokuality.server.utils.OSUtils;

import java.io.File;

public class GlobalDependencyInstaller {

	public static boolean isTesseractInstalled() {
		File tesseract = new File(OSUtils.getBinaryPath("tesseract"));

		boolean installed = (tesseract != null && tesseract.exists() && tesseract.isFile());
		Log.getRootLogger().info("Tesseract installed " + installed, new Object[] {});
		return installed;
	}

	public static boolean isNodeInstalled() {
		File node = new File(OSUtils.getBinaryPath("node"));

		boolean installed = (node != null && node.exists() && node.isFile());
		Log.getRootLogger().info("Node installed " + installed, new Object[] {});
		return installed;
	}

	public static boolean isGoInstalled() {
		File go = new File(OSUtils.getBinaryPath("go"));

		boolean installed = (go != null && go.exists() && go.isFile());
		Log.getRootLogger().info("Go installed " + installed, new Object[] {});
		return installed;
	}

	public static boolean isFFMPEGInstalled() {
		File ffmpeg = FFMPEGConstants.FFMPEG;
		boolean installed = (ffmpeg.exists() && ffmpeg.isFile());
		Log.getRootLogger().info("FFMPEG installed " + installed, new Object[] {});
		return installed;
	}

	public static void installFFMPEG() {
		Log.getRootLogger().info("Installing FFMPEG", new Object[] {});
		DependencyManager dependencyManager = new DependencyManager();
		if (!OSUtils.isWindows()) {
			Boolean dependencyDownloaded = dependencyManager.downloadDependency(FFMPEGConstants.FFMPEG);
			if (dependencyDownloaded) {
				FFMPEGConstants.FFMPEG.setExecutable(true);
			}
		}

		if (OSUtils.isWindows()) {
			String ffmpegZipName = FFMPEGConstants.FFMPEG_WIN_ZIP.getName();
			File ffmpegZipFile = new File(
					DependencyConstants.DEPENDENCY_DIR.getAbsolutePath() + File.separator + ffmpegZipName);
			Boolean dependencyDownloaded = dependencyManager.downloadDependency(ffmpegZipFile);
			if (dependencyDownloaded) {
				dependencyManager.unzipDependency(ffmpegZipFile);
			}
		}
	}

	public static boolean isTesseractTrainedDataInstalled() {
		File tesseract = TesseractConstants.TESSERACT_TESS_DATA_DIR;
		boolean installed = (tesseract.exists() && tesseract.isDirectory());
		Log.getRootLogger().info("Tesseract trained data installed " + installed, new Object[] {});
		return installed;
	}

	public static void installTesseractTrainedData() {
		Log.getRootLogger().info("Installing Tesseract trained data", new Object[] {});
		DependencyManager dependencyManager = new DependencyManager();
		String zipName = TesseractConstants.TESS_DATA_ZIP_NAME;
		File tesseractZipFile = new File(
				DependencyConstants.DEPENDENCY_DIR.getAbsolutePath() + File.separator + zipName);
		Boolean dependencyDownloaded = dependencyManager.downloadDependency(tesseractZipFile);
		if (dependencyDownloaded) {
			dependencyManager.unzipDependency(tesseractZipFile);
		}
	}

	public static boolean isHarmonyInstalled() {
		File harmony = DependencyConstants.HARMONY_BIN;
		boolean installed = (harmony.exists() && harmony.isFile());
		Log.getRootLogger().info("Harmony CLI installed " + installed, new Object[] {});
		return installed;
	}

	public static void installHarmony() {
		Log.getRootLogger().info("Installing Harmony CLI", new Object[] {});
		DependencyManager dependencyManager = new DependencyManager();
		String zipName = DependencyConstants.HARMONY_ZIP_NAME;
		File harmonyZipFile = new File(DependencyConstants.DEPENDENCY_DIR.getAbsolutePath() + File.separator + zipName);
		Boolean dependencyDownloaded = dependencyManager.downloadDependency(harmonyZipFile);
		if (dependencyDownloaded) {
			dependencyManager.unzipDependency(harmonyZipFile);
		}
	}

	public static boolean isRokuWebDriverInstalled() {
		boolean installed = RokuWebDriverConstants.MAIN.exists();
		Log.getRootLogger().info("Roku WebDriver installed: " + installed);
		return installed;
	}

	public static void installRokuWebDriver() {
		Log.getRootLogger().info("Installing Roku WebDriver", new Object[] {});
		DependencyManager dependencyManager = new DependencyManager();
		boolean dependencyDownloaded = dependencyManager.downloadDependency(RokuWebDriverConstants.ROKU_WEBDRIVER_ZIP);
		dependencyManager.unzipDependency(RokuWebDriverConstants.ROKU_WEBDRIVER_ZIP);
		if (dependencyDownloaded) {
			String goPath = OSUtils.getBinaryPath("go");
			String[] command = null;
			File buildFile = new File(
					DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + "BuildRokuWebDriver.sh");
			String buildContent = "";
			if (OSUtils.isWindows()) {
				buildFile = new File(buildFile.getAbsolutePath().replace(".sh", ".bat"));
				buildContent = "cd " + RokuWebDriverConstants.ROKU_WEBDRIVER_BASE_DIR + File.separator + "src"
						+ System.lineSeparator() + goPath + " env -w GOPATH="
						+ RokuWebDriverConstants.ROKU_WEBDRIVER_BASE_DIR.getAbsolutePath() + System.lineSeparator()
						+ goPath + " get github.com/gorilla/mux" + System.lineSeparator() + goPath
						+ " get github.com/sirupsen/logrus" + System.lineSeparator() + goPath + " build main.go";

				File logStartFile = LogFileUtils.getLogFile("buildrokuwebdriver.log");
				logStartFile = LogFileUtils.cleanLogFile(logStartFile);
				FileUtils.createFile(logStartFile);
				command = new String[] { "cmd", "/c", "start", "/b", "\"\"", ">" + logStartFile.getAbsolutePath(),
						buildFile.getAbsolutePath() };
			} else {
				String userPath = OSUtils.getPathVar();
				buildContent = "# !/bin/bash" + System.lineSeparator() + "export PATH=" + userPath
						+ System.lineSeparator() + "cd " + RokuWebDriverConstants.ROKU_WEBDRIVER_BASE_DIR
						+ File.separator + "src" + System.lineSeparator() + "export GOPATH="
						+ RokuWebDriverConstants.ROKU_WEBDRIVER_BASE_DIR.getAbsolutePath() + System.lineSeparator()
						+ goPath + " get github.com/gorilla/mux" + System.lineSeparator() + goPath
						+ " get github.com/sirupsen/logrus" + System.lineSeparator() + goPath + " build main.go";

				command = new String[] { "bash", buildFile.getAbsolutePath() };
			}

			FileUtils.writeStringToFile(buildFile, buildContent);
			new CommandExecutor().execCommand(String.join(" ", command), null);
		}

	}

}
