package com.constanttherapy.share.tasks.plain;

import java.util.ArrayList;
import java.util.List;

/** POJO that represents the various possible aspects of an instruction, stimulus, or
 * response ... e.g. textual instructions would include an aspect containing non-null text field
 * ... note that each aspect must have at least one modality e.g. audio image or text */
public class PlainTaskAspect {

	/** audio clip for this aspect - OK to be null if no audio provided */
	public List<String> audioPathElementList = new ArrayList<String>();
	/** image for this aspect - may be null if no image provided */
	public String imagePathElement = null;
	/** text string for this aspect - may be null if no text provided */
	public String textElement = null;
	/** text string for this aspect - may be null if no text provided */
	public String htmlPathElement = null;
	public String contentType = null;
	/**
	 * Value for assessment task choice
	 */
	public Integer value = null;

	/** image rotation, defaults to none */
	public int imageRotation = 0;
	public boolean isRotatable = false;

	public boolean hasText() { return (this.textElement != null);}
	public boolean hasAudio() { return (this.audioPathElementList.size() > 0); }
	public boolean hasImage() { return (this.imagePathElement != null); }
	public boolean hasHtml() { return (this.htmlPathElement != null); }

	/** audio is a list, because we often play hodgepodges of audio clips in sequence
	 * ... this method adds another clip to our list */
	public void addAdditionalAudio(String moreAudio) {
		this.audioPathElementList.add(moreAudio);
	}

	public static PlainTaskAspect createWithText(String theText) {

		PlainTaskAspect theAspect = new PlainTaskAspect();
		theAspect.textElement = theText;

		return theAspect;
	}

	public static PlainTaskAspect createWithTextAndAudio(String theText, String theAudioPath) {

		PlainTaskAspect theAspect = new PlainTaskAspect();
		theAspect.textElement = theText;
		theAspect.audioPathElementList.add(theAudioPath);

		return theAspect;
	}

	public static PlainTaskAspect createWithAudio(String theAudioPath) {

		PlainTaskAspect theAspect = new PlainTaskAspect();
		theAspect.audioPathElementList.add(theAudioPath);

		return theAspect;
	}

	public static PlainTaskAspect createWithImage(String theImagePath) {

		PlainTaskAspect theAspect = new PlainTaskAspect();
		theAspect.imagePathElement = theImagePath;

		return theAspect;
	}

	public static PlainTaskAspect createWithImage(String theImagePath, String theHtmlPath) {

		PlainTaskAspect theAspect = new PlainTaskAspect();
		theAspect.imagePathElement = theImagePath;
		theAspect.htmlPathElement = theHtmlPath;
		theAspect.contentType = "html";

		return theAspect;
	}

	public static PlainTaskAspect createWithImageRotated(String theImagePath, int rotation) {

		PlainTaskAspect theAspect = new PlainTaskAspect();
		theAspect.imagePathElement = theImagePath;
		theAspect.imageRotation = rotation;
		theAspect.isRotatable = true;

		return theAspect;
	}

}
