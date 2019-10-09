package com.rokuality.server.core.ocr;

import java.awt.Point;
import java.util.Objects;

public class ImageText {

    private String text;
    private Point location;
    private double length;
	private double width;
	private float confidence;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
	}
	
	public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ImageText imageText = (ImageText) o;
		return Double.compare(imageText.length, length) == 0 && Double.compare(imageText.confidence, confidence) == 0 
				&& Double.compare(imageText.width, width) == 0 && Objects
                .equals(text, imageText.text) && Objects.equals(location, imageText.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, location, length, width, confidence);
    }

    @Override
    public String toString() {
        return "{" + "\"text\":\"" + text + '"' + "," + "\"confidence\":\"" + confidence + '"' +",\"location\":" + "{\"x\":" + location.getX() + "," + "\"y\":"
                + location.getY() + "}" + ",\"length\":" + length + ",\"width\":" + width + '}';
    }
}
