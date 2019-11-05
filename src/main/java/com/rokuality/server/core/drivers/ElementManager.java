package com.rokuality.server.core.drivers;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.rokuality.server.constants.SessionConstants;

import org.json.simple.JSONObject;

public class ElementManager {

	private static Queue<Map<String, JSONObject>> elementMap = new ConcurrentLinkedQueue<Map<String, JSONObject>>();

	public static void addElement(String sessionID, JSONObject element) {
		Map<String, JSONObject> map = new HashMap<>();
		map.put(sessionID, element);
		elementMap.add(map);
	}

	public static void removeElements(String sessionID) {
		for (Map<String, JSONObject> map : elementMap) {
			if (map.containsKey(sessionID)) {
				elementMap.remove(map);
				break;
			}
		}
	}

	public static JSONObject getElement(String sessionID, String elementID) {
		for (Map<String, JSONObject> map : elementMap) {
			if (map.containsKey(sessionID) && map.get(sessionID).get(SessionConstants.ELEMENT_ID).equals(elementID)) {
				return map.get(sessionID);
			}
		}

		return null;
	}

}
