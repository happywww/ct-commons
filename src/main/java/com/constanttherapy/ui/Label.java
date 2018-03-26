package com.constanttherapy.ui;

public class Label {
	
	private String text;
	private Color textColor;
	
	private Color glowColor;
	private boolean hidden;

	public Label(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
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
