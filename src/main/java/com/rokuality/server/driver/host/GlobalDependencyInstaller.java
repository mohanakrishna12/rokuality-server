package com.rokuality.server.driver.host;

import org.eclipse.jetty.util.log.Log;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.FFMPEGConstants;
import com.rokuality.server.constants.TesseractConstants;
import com.rokuality.server.core.DependencyManager;
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
            File ffmpegZipFile = new File(DependencyConstants.DEPENDENCY_DIR.getAbsolutePath() + File.separator + ffmpegZipName);
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
        File tesseractZipFile = new File(DependencyConstants.DEPENDENCY_DIR.getAbsolutePath() + File.separator + zipName);
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

}
