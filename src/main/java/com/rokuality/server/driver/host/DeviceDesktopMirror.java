package com.rokuality.server.driver.host;

import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import java.awt.*;
import java.awt.image.BufferedImage;

import com.rokuality.server.core.ImageCollector;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.enums.SessionCapabilities;
import com.rokuality.server.utils.ImageUtils;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;

public class DeviceDesktopMirror {

	private static boolean sessionStarted = false;
	private static int width = -1;
	private static int height = -1;

	public static void initMirror(String sessionID, String mirrorCap) {
		try {
			mirrorCap = mirrorCap.toLowerCase().replace(" ", "");
			width = Integer.parseInt(mirrorCap.split("x")[0]);
			height = Integer.parseInt(mirrorCap.split("x")[1]);
		} catch (Exception e) {
			Log.getRootLogger()
					.warn(String.format(
							"%s capability is not in proper 'widthxheight' format and mirroring will not start!",
							SessionCapabilities.MIRROR_SCREEN.value()));
			Log.getRootLogger().warn(e);
			return;
		}

		Log.getRootLogger().info(String.format("Initiating screen mirror with width %s and height %s", width, height));
		sessionStarted = true;

		JFrame frame = new JFrame();
		frame.setBounds(1, 1, width, height);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		//frame.setAlwaysOnTop(true);
		frame.getContentPane().setLayout(null);

		JLabel imageLabel = new JLabel("Preparing screen image...");
		imageLabel.setBounds(1, 1, width, height);
		frame.add(imageLabel);

		new Thread() {
			@Override
			public void run() {
				ImageCollector imageCollector = SessionManager.getImageCollector(sessionID);
				while (sessionStarted) {
					File capturedImage = imageCollector.viewCurrentImage(true);
					if (capturedImage != null && capturedImage.exists()) {
						BufferedImage bufferedImage = null;
						try {
							bufferedImage = ImageUtils.getBufferedImage(capturedImage);
						} catch (IOException e) {
							Log.getRootLogger().warn(e);
						}

						if (bufferedImage != null) {
							ImageIcon icn = new ImageIcon(
									bufferedImage.getScaledInstance(width, height, Image.SCALE_DEFAULT));
							imageLabel.setIcon(icn);
						}
					}
				}
			};
		}.start();

		frame.setVisible(true);

		new Thread() {
			@Override
			public void run() {
				while (frame.isDisplayable() && SessionManager.getSessionInfo(sessionID) != null) {
					SleepUtils.sleep(1000);
				}

				// error or timeout - close
				if (frame.isDisplayable() && SessionManager.getSessionInfo(sessionID) == null) {
					sessionStarted = false;
					frame.dispose();
				} else if (sessionStarted) {
					// user closed the frame entirely - terminate the session entirely
					sessionStarted = false;
					frame.dispose();
				}
			};
		}.start();
	}

}