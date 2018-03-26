package com.constanttherapy.share.tasks.plain;

import java.util.List;

public class PlainTaskAspectedObject {
	/** text/image/audio that serves as the primary form of display */
	public PlainTaskAspect primaryAspect = null;
	/** some task types have secondary formats displayed/played in addition to the
	 * primary form */
	public PlainTaskAspect otherAspect = null;
	
	/** detect the image from either primary or other aspect - note that ignoring whether it's primary
	 * or other is only appropriate in some situations, since primary vs. other really matters e.g.
	 * in general audio should be played automatically IFF it is primary */
	public boolean hasImage() { 
		return (primaryAspect.hasImage() 
				|| ((otherAspect != null) && (otherAspect.hasImage()))); 
	}
	
	/** detect the image's rotatability, if there is one - false if either no image or image provided
	 * but is not rotatable */
	public boolean hasRotatableImage() { 
		return hasImage() && (primaryAspect.isRotatable 
				|| ((otherAspect != null) && (otherAspect.isRotatable))); 
	}
	
	/** detect the audio from either primary or other aspect - note that ignoring whether it's primary
	 * or other is only appropriate in some situations, since primary vs. other really matters e.g.
	 * in general audio should be played automatically IFF it is primary */
	public boolean hasAudio() { 
		return (primaryAspect.hasAudio() 
				|| ((otherAspect != null) && (otherAspect.hasAudio()))); 
	}
	
	/** detect the text from either primary or other aspect - note that ignoring whether it's primary
	 * or other is only appropriate in some situations, since primary vs. other really matters e.g.
	 * in general audio should be played automatically IFF it is primary */
	public boolean hasText() { 
		return (primaryAspect.hasText() 
				|| ((otherAspect != null) && (otherAspect.hasText()))); 
	}

	/** get the text from either primary or other aspect - note that ignoring whether it's primary
	 * or other is only appropriate in some situations, since primary vs. other really matters e.g.
	 * in general audio should be played automatically IFF it is primary */
	public String getText() {
		if (primaryAspect.hasText()) return primaryAspect.textElement;
		else if ((otherAspect != null) && (otherAspect.hasText())) return otherAspect.textElement;
		else return null;
	}

	/** get the image from either primary or other aspect - note that ignoring whether it's primary
	 * or other is only appropriate in some situations, since primary vs. other really matters e.g.
	 * in general audio should be played automatically IFF it is primary */
	public String getImagePath() {
		if (primaryAspect.hasImage()) return primaryAspect.imagePathElement;
		else if ((otherAspect != null) && (otherAspect.hasImage())) return otherAspect.imagePathElement;
		else return null;
	}
	
	/** get the rotation of the image of the primary aspect if it has an image, otherwise of the other
	 * aspect, and if that's not an image, returns zero */
	public int getImageRotation() {
		if (primaryAspect.hasImage()) return primaryAspect.imageRotation;
		else if ((otherAspect != null) && (otherAspect.hasImage())) return otherAspect.imageRotation;
		else return 0;
	}

	/** get the audio paths from either primary or other aspect - note that ignoring whether it's primary
	 * or other is only appropriate in some situations, since primary vs. other really matters e.g.
	 * in general audio should be played automatically IFF it is primary */
	public List<String> getAudioPaths() {
		if (primaryAspect.hasAudio()) return primaryAspect.audioPathElementList;
		else if ((otherAspect != null) && (otherAspect.hasAudio())) return otherAspect.audioPathElementList;
		else return null;
	}
}
