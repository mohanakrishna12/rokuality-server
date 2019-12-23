package com.rokuality.server.main;

import com.google.common.reflect.ClassPath;
import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.LogConstants;
import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.core.scheduledtasks.ExpiredSessionTask;
import com.rokuality.server.driver.host.GlobalDependencyInstaller;
import com.rokuality.server.utils.CacheUtils;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.LogFileUtils;
import com.rokuality.server.utils.OSUtils;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.RolloverFileOutputStream;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.PrintStream;
import java.util.Set;
import java.util.Timer;

import org.sikuli.basics.Debug;
import org.sikuli.basics.Settings;

@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
public class ServerMain {

	private static final int DEFAULT_SERVER_PORT = 7777;
	private static final int DEFAULT_THREAD_SIZE = 10;

	public static void main(String[] args) throws Exception {

		File logDir = LogConstants.LOG_DIR;
		if (!logDir.exists()) {
			FileUtils.createDirectory(logDir);
		}

		RolloverFileOutputStream logOutputStream = new RolloverFileOutputStream(
				logDir.getAbsolutePath() + File.separator + LogConstants.SERVER_EVENT_LOG_NAME, true);
		PrintStream logStream = new PrintStream(logOutputStream);
		System.setOut(logStream);
		System.setErr(logStream);

		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(getThreadSize());
		Server server = new Server(threadPool);

		NCSARequestLog requestLog = new NCSARequestLog(
				logDir.getAbsolutePath() + File.separator + LogConstants.SERVER_REQUEST_LOG_NAME);
		requestLog.setAppend(true);
		requestLog.setExtended(false);
		requestLog.setLogTimeZone("GMT");
		requestLog.setLogLatency(true);
		requestLog.setRetainDays(2);
		server.setRequestLog(requestLog);

		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
		connector.setPort(getServerPort());
		server.addConnector(connector);

		ServletHandler handler = new ServletHandler();
		server.setHandler(handler);

		Set<ClassPath.ClassInfo> servletClasses = ClassPath.from(ServerMain.class.getClassLoader())
				.getTopLevelClassesRecursive(ServerConstants.SERVLETS_PACKAGE);
		servletClasses.forEach(servletClass -> {
			Class servlet = servletClass.load();
			if (servlet.getSuperclass().equals(HttpServlet.class)) {
				handler.addServletWithMapping(servlet, "/" + servlet.getSimpleName());
			} else {
				Log.getRootLogger().warn("Not registering " + servlet.getSimpleName() + " since it's not a servlet!",
						new Object[] {});
			}
		});

		server.start();

		CacheUtils.initCache();

		FileUtils.createDirectory(DependencyConstants.DEPENDENCY_DIR);
		FileUtils.createDirectory(DependencyConstants.TEMP_DIR);
		FileUtils.cleanDirectory(DependencyConstants.TEMP_DIR);
		Log.getRootLogger().info(String.format("Temp directory set to '%s'.", DependencyConstants.TEMP_DIR.getAbsolutePath()));

		boolean ffmpegInstalled = GlobalDependencyInstaller.isFFMPEGInstalled();
		boolean tesseractInstalled = GlobalDependencyInstaller.isTesseractInstalled();
		boolean tesseractTrainedDataInstalled = GlobalDependencyInstaller.isTesseractTrainedDataInstalled();
		boolean harmonyInstalled = GlobalDependencyInstaller.isHarmonyInstalled();
		boolean rokuWebDriverInstalled = GlobalDependencyInstaller.isRokuWebDriverInstalled();

		if (!ffmpegInstalled || !tesseractTrainedDataInstalled || !harmonyInstalled || !rokuWebDriverInstalled) {
			try {
				OSUtils.displaySystemMessage("Performing some setup. Will let you know when we're ready...");
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}
		}

		if (!ffmpegInstalled) {
			GlobalDependencyInstaller.installFFMPEG();
		}

		if (!harmonyInstalled) {
			GlobalDependencyInstaller.installHarmony();
		}

		if (!rokuWebDriverInstalled) {
			GlobalDependencyInstaller.installRokuWebDriver();
		}

		if (!tesseractInstalled) {
			Log.getRootLogger().warn("Tesseract is NOT installed! It must be installed and "
					+ "available on your path. It's most easily installed via 'brew install tesseract' "
					+ "for Mac, and 'scoop install tesseract' for Windows.");
		}

		if (!tesseractTrainedDataInstalled) {
			GlobalDependencyInstaller.installTesseractTrainedData();
		}

		try {
			File sikuliLog = LogFileUtils.getLogFile("sikuli.log");
			
			Settings.DebugLogs = true;
			Settings.InfoLogs = true;
			Settings.ActionLogs = true;
			Debug.setDebugLevel(3);
			Debug.setLogFile(sikuliLog.getAbsolutePath());
		} catch (Exception e) {

		}

		try {
			Timer timer = new Timer();
			ExpiredSessionTask expiredSessionTask = new ExpiredSessionTask();
			timer.schedule(expiredSessionTask, 1000, ServerConstants.EXPIRED_SESSION_INTERVAL_MS);
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		try {
			OSUtils.displaySystemMessage("Server is listening at http://localhost:" + getServerPort());
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}
		
		Log.getRootLogger().info("Server is ready.");
		server.join();

	}

	public static int getServerPort() {
		String portStr = System.getProperty("port");
		if (portStr != null) {
			try {
				return Integer.parseInt(portStr);
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}
		}

		return DEFAULT_SERVER_PORT;
	}

	private static int getThreadSize() {
		String portStr = System.getProperty("threads");
		if (portStr != null) {
			try {
				return Integer.parseInt(portStr);
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}
		}

		return DEFAULT_THREAD_SIZE;
	}
}
