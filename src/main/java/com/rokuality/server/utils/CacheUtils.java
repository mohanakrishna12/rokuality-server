package com.rokuality.server.utils;

import java.util.concurrent.TimeUnit;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.eclipse.jetty.util.log.Log;

public class CacheUtils {

	private static final String CACHE_NAME = "servercache";
	private static final int MAX_CACHE_ENTRIES = 10000;
	private static final int MAX_CACHE_TTL_HOURS = 2;

	private static Cache<String, String> cache = null;

	public static void initCache() {
		Log.getRootLogger().info("Initiating global server cache.", new Object[]{});
		cache = new Cache2kBuilder<String, String>() {}
			.name(CACHE_NAME)
			.expireAfterWrite(MAX_CACHE_TTL_HOURS, TimeUnit.HOURS)
			.entryCapacity(MAX_CACHE_ENTRIES)
			.build();
	}

	public static boolean exists(String key) {
		return (cache != null && cache.containsKey(key));
	}

	public static void add(String key, String value) {
		if (cache != null && key != null && value != null) {
			cache.put(key, value);
		}
	}

	public static void remove(String key) {
		if (cache != null) {
			cache.remove(String.valueOf(key));
		}
	}

	public static String get(String key) {
		if (cache == null) {
			return null;
		}
		return cache.peek(key);
	}

}
