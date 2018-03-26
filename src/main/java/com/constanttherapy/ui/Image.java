package com.constanttherapy.ui;

public class Image {
	
	private String imagePath;
	private Color glowColor;
	private boolean hidden;

	public Image(String imagePath) {
		this.imagePath = imagePath;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public Color getGlowColor() {
		return glowColor;
	}

	public void setGlowColor(Color glowColor) {
		this.glowColor = glowColor;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
}