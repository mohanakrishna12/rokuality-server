package com.rokuality.server.core;

import com.google.common.reflect.ClassPath;
import com.rokuality.server.constants.ServerConstants;

import org.eclipse.jetty.util.log.Log;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public class AnnotationReader {

	// TODO - clean up and remove over-encompassing exception block
	@SuppressWarnings( {"rawtypes", "unchecked"})
	public static String getAnnotationValue(String className, Class annotationClass, String attributeName) {

		String annotationValue = System.getProperty("rokuality." + className + "." 
				+ annotationClass.getSimpleName() + "." + attributeName);
		if (annotationValue != null) {
			return annotationValue;
		}

		Set<ClassPath.ClassInfo> servletClasses = new HashSet<>();
		try {
			servletClasses = ClassPath.from(AnnotationReader.class.getClassLoader())
					.getTopLevelClassesRecursive(ServerConstants.SERVLETS_PACKAGE);

			if (!servletClasses.isEmpty()) {
				for (ClassPath.ClassInfo classInfo : servletClasses) {
					Class servlet = classInfo.load();
					if (servlet.getSimpleName().equals(className)) {
						Annotation annotation = servlet.getAnnotation(annotationClass);
        				if (annotation != null) {
							annotationValue = String.valueOf(annotation.annotationType().getMethod(attributeName).invoke(annotation));
							System.setProperty("rokuality." + className + "." 
									+ annotationClass.getSimpleName() + "." + attributeName, annotationValue.toString());
        				}
						break;
					}
				}
			}
		} catch (Exception e) {
			Log.getRootLogger().warn("Failed to retrieve annotation", e);
		}

		return annotationValue;
	}

}
