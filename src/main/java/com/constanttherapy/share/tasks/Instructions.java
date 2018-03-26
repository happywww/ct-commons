package com.constanttherapy.share.tasks;

import java.util.List;

/**
 * Created by connormathews on 7/8/15.
 * Modified by Andy on 3/14/18, using new generic structure
 */
public class Instructions {
    public enum Position {
        primary,
        secondary,
        top
    }

    public Position position;
    public Boolean showAudioSignifier;

    /**
     * Text of instruction that will be displayed.
     */
    public String text;
    /**
     * List of instruction paths that will be played.
     */
    public List<String> instructionAudioPaths;

    /**
     * Whether instructions will automatically be played when view is displayed.
     */
    public boolean autoplayInstructions;

    public Instructions() {
        this.text = null;
        this.instructionAudioPaths = null;
        this.autoplayInstructions = true;
    }
}
