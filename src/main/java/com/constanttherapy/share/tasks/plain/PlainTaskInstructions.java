package com.constanttherapy.share.tasks.plain;

import java.util.List;

/** instructions provided to the user for a task - may have multiple aspects
 * e.g. may be presented in more than one way e.g. text or image */
public class PlainTaskInstructions extends PlainTaskAspectedObject {

	/** if false, instructions are played at the discretion of the task implementation
	 * and not always played immediately on entering the task */
	public Boolean autoPlayInstructions = false;
	
	/** populate the instructions portion of our genericized serialization object version of
	 * this task, with text and two audio paths */
	public static PlainTaskInstructions create(String theText, String audio1, String audio2) {
				
		PlainTaskInstructions returnVal = create(theText, audio1);
		returnVal.otherAspect.addAdditionalAudio(audio2);
		return returnVal;

	}
	
	/** populate the instructions portion of our genericized serialization object version of
	 * this task, with text and multiple audio paths */
	public static PlainTaskInstructions create(String theText, List<String> audioPaths) {
				
		PlainTaskInstructions returnVal = create(theText, audioPaths.get(0));
		for (int q = 1; q < audioPaths.size(); q++) 
			returnVal.otherAspect.addAdditionalAudio(audioPaths.get(q));
		return returnVal;

	}
	
	/** populate the instructions portion of our genericized seralization object version of
	 * this task, with text and a single audio path */
	public static PlainTaskInstructions create(String theText, String audioPath) {
		
		PlainTaskInstructions returnVal = new PlainTaskInstructions();
	
		returnVal.autoPlayInstructions = true;
		// in the old format the instructions were created on the client side by playing 
		// a client-defined audio file and then a server-supplied audio file ...
		// TODO internationalization via a ResourceBundle, using a locale/language spec'd by the client
		// ... but then, even the word itself would need that same treatment ... may also want to
		// try hirondelle.web4j.ui.translate
		returnVal.primaryAspect = PlainTaskAspect.createWithText(theText);
		// the current iPad client has hardcoded literals for the does_this_rhyme_with.mp3, rhymes_with.mp3 
		// and does_not_rhyme_with.mp3 audio files, but we don't want to put that on the client side anymore, 
		// so we sent multipart audio files ... the client will play them in succession
		returnVal.otherAspect = PlainTaskAspect.createWithAudio(audioPath);
		return returnVal;

	}
	
	/** populate instructions with audio only */
	public static PlainTaskInstructions createAudioOnly(String audioPath) {
		PlainTaskInstructions returnVal = new PlainTaskInstructions();
		returnVal.primaryAspect = PlainTaskAspect.createWithAudio(audioPath);		
		return returnVal;
	}

}
