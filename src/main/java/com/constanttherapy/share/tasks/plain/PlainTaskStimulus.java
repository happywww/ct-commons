package com.constanttherapy.share.tasks.plain;

/** stimulus for a task e.g. a map */
public class PlainTaskStimulus extends PlainTaskAspectedObject {

	public static PlainTaskStimulus createWithTextPrimaryAspect(String theText) {
		
		PlainTaskStimulus theStimulus = new PlainTaskStimulus();
		theStimulus.primaryAspect = PlainTaskAspect.createWithText(theText);
		
		return theStimulus;
	}

	public static PlainTaskStimulus createWithAudioPrimaryAspect(String audioPath) {
		
		PlainTaskStimulus theStimulus = new PlainTaskStimulus();
		theStimulus.primaryAspect = PlainTaskAspect.createWithAudio(audioPath);
		
		return theStimulus;
	}

	public void setOtherAspectText(String theText) {
		
		otherAspect = PlainTaskAspect.createWithText(theText);
		
	}

	public static PlainTaskStimulus createWithImagePrimaryAspect(String theImagePath) {
		
		PlainTaskStimulus theStimulus = new PlainTaskStimulus();
		theStimulus.primaryAspect = PlainTaskAspect.createWithImage(theImagePath);
		
		return theStimulus;
	}

	public static PlainTaskStimulus createWithImagePrimaryAspect(String theImagePath, String theHtmlPath) {

		PlainTaskStimulus theStimulus = new PlainTaskStimulus();
		theStimulus.primaryAspect = PlainTaskAspect.createWithImage(theImagePath, theHtmlPath);

		return theStimulus;
	}
	
	public static PlainTaskStimulus createWithRotatedImagePrimaryAspect(String theImagePath, int rotation) {
		
		PlainTaskStimulus theStimulus = new PlainTaskStimulus();
		theStimulus.primaryAspect = PlainTaskAspect.createWithImageRotated(theImagePath, rotation);
		
		return theStimulus;
	}
	
	public static PlainTaskStimulus createWithImagePrimaryAndAudioTextOtherAspects(String theImagePath, String theText, String theAudioPath) {
		
		PlainTaskStimulus theStimulus = new PlainTaskStimulus();
		theStimulus.primaryAspect = PlainTaskAspect.createWithImage(theImagePath);
		theStimulus.otherAspect = PlainTaskAspect.createWithTextAndAudio(theText, theAudioPath);
		
		return theStimulus;

	}

}
