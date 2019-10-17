package com.rokuality.server.core;

import java.io.File;
import java.util.Map;

import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.driver.device.hdmi.HDMIScreenManager;
import com.rokuality.server.enums.PlatformType;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

public class ExpiredSessionHandler {

	public static void initMonitoringThread() {
		Log.getRootLogger().info("Starting expired session monitoring timer.");
		Integer timer = 0;
		while (true) {
			timer++;
			if (timer > ServerConstants.EXPIRED_SESSION_INTERVAL_S) {
				try {
					handleExpiredSessions();
				} catch (Exception e) {
					Log.getRootLogger().warn(e);
				}

				timer = 0;
			}
			SleepUtils.sleep(1000);
		}
	}

	public static void handleExpiredSessions() {
		Map<String, Long> entries = SessionManager.getAllSessionActivityEntries();
		for (Map.Entry<String, Long> entry : entries.entrySet()) {
			Long lastActivity = entry.getValue();
			long currentTime = System.currentTimeMillis();
			if ((currentTime - lastActivity) / 1000 > getExpiredSessionTimeout()) {
				Log.getRootLogger().warn(String.format("Orphaned session %s found. Terminating!", entry.getKey()));

				ImageCollector collector = SessionManager.getImageCollector(entry.getKey());
				if (collector != null) {
					collector.stopRecording();
				}

				Object sessionInfoObj = SessionManager.getSessionInfo(entry.getKey());
				if (sessionInfoObj != null) {
					JSONObject sessionInfo = (JSONObject) sessionInfoObj;

					if (PlatformType.HDMI.equals(sessionInfo.get(SessionConstants.PLATFORM))) {
						File videoCapture = new File(String.valueOf(sessionInfo.get(SessionConstants.VIDEO_CAPTURE_FILE)));
						HDMIScreenManager.stopVideoCapture(videoCapture);
						FileUtils.deleteFile(videoCapture);
					}

					String capturePath = String.valueOf(sessionInfo.get(SessionConstants.IMAGE_COLLECTION_DIRECTORY));
					File captureDir = new File(capturePath);
					if (captureDir.exists()) {
						FileUtils.deleteDirectory(captureDir);
					}
				}
				
				SessionManager.removeGoogleCredentials(entry.getKey());
				SessionManager.removeImageCollector(entry.getKey());
				SessionManager.removeSessionActivity(entry.getKey());
				SessionManager.removeSessionInfo(entry.getKey());
			}
		}
	}

	private static int getExpiredSessionTimeout() {
		String userTimeout = System.getProperty("commandtimeout");
		int timeout = ServerConstants.EXPIRED_SESSION_TIMEOUT_DEFAULT_S;
		if (userTimeout != null) {
			try {
				timeout = Integer.parseInt(userTimeout);
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}
		}

		return timeout;
	}

}