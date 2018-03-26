package com.constanttherapy.share.tasks.plain;

/* an option for the user to choose as their response, either test, audio, or an
 * image ... may provide more than one mode to the user e.g. primary may be some text,
 * but if they press a button they may also get audio from the otherAspect */
public class PlainTaskChoice extends PlainTaskAspectedObject {

	public Boolean isCorrect = false;
	
	public static PlainTaskChoice createWithTextAspect(String theText, Boolean correctness) {
		
		PlainTaskChoice theChoice = new PlainTaskChoice();
		theChoice.primaryAspect.textElement = theText;
		theChoice.isCorrect = correctness;
		
		return theChoice;
	}
	
	public static PlainTaskChoice createWithAspect(PlainTaskAspect theAspect, Boolean correctness) {
		
		PlainTaskChoice theChoice = new PlainTaskChoice();
		
		theChoice.primaryAspect = theAspect;
		theChoice.isCorrect = correctness;
		
		return theChoice;
	}
	
}
