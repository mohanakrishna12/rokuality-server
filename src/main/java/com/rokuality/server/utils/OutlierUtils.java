package com.rokuality.server.utils;

import java.util.ArrayList;
import java.util.List;

public class OutlierUtils {

	private static double getMean(List<Integer> values) {
		int sum = 0;
		for (int value : values) {
			sum += value;
		}
	
		return (sum / values.size());
	}
	
	private static double getVariance(List<Integer> values) {
		double mean = getMean(values);
		int temp = 0;
	
		for (int a : values) {
			temp += (a - mean) * (a - mean);
		}
	
		return temp / (values.size() - 1);
	}
	
	private static double getStdDev(List<Integer> values) {
		return Math.sqrt(getVariance(values));
	}
	
	public static List<Integer> eliminateOutliers(List<Integer> values, float scaleOfElimination) {
		double mean = getMean(values);
		double stdDev = getStdDev(values);
	
		final List<Integer> newList = new ArrayList<>();
	
		for (int value : values) {
			boolean isLessThanLowerBound = value < mean - stdDev * scaleOfElimination;
			boolean isGreaterThanUpperBound = value > mean + stdDev * scaleOfElimination;
			boolean isOutOfBounds = isLessThanLowerBound || isGreaterThanUpperBound;
	
			if (!isOutOfBounds) {
				newList.add(value);
			}
		}
	
		int countOfOutliers = values.size() - newList.size();
		if (countOfOutliers == 0) {
			return values;
		}
	
		return newList;
	}

}
