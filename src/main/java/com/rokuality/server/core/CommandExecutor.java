package com.rokuality.server.core;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;

public class CommandExecutor {

	private static final String ERROR_MSG = "Command failed to execute: ";
	private static final Integer DEFAULT_TIMEOUT_SEC = 20;

	private String command = null;
	private String output = null;
	private String failureMsg = null;
	
	public String getCommand() {
		return command;
	}

	public String getOutput() {
		return output;
	}

	public String execCommand(String command, Integer timeoutInSec) {
		long timeout = (long) setTimeout(timeoutInSec);
		this.command = command;

		CommandLine commandLine = CommandLine.parse(command);
		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
		Executor executor = new DefaultExecutor();
	
		ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout * 1000);
		executor.setWatchdog(watchdog);
	
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
    		executor.setStreamHandler(streamHandler);
			executor.execute(commandLine, resultHandler);
			resultHandler.waitFor();
			this.output = outputStream.toString();
			return output;
		} catch (Exception e) {
			failureMsg = ERROR_MSG + this.command + e.getMessage();
			return null;
		}
	}

	public String getFailureMsg() {
		return failureMsg;
	}

	private Integer setTimeout(Integer timeoutInSec) {
		if (timeoutInSec == null) {
			return DEFAULT_TIMEOUT_SEC;
		}
		return timeoutInSec;
	}

}
