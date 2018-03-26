package com.constanttherapy.share.tasks.plain;

import com.constanttherapy.share.taskdata.TaskData;

import java.util.ArrayList;
import java.util.List;

public class PlainTask extends TaskData
{
    /**
     * Difficulty level of this task
     */
    public Integer level;

    /**
     * instructions presented to the user, may be string/image/sound
     */
    public PlainTaskInstructions theInstructions = new PlainTaskInstructions();

    /**
     * stimuli e.g. a map or a voicemail
     */
    public List<PlainTaskStimulus> stimuli = new ArrayList<PlainTaskStimulus>();

    /**
     * response choices provided as options to the user - will be more than one for multiple choice
     */
    public PlainTaskPresentedResponses responses = new PlainTaskPresentedResponses();

    private List<String> addPrefixToSuffixes(List<String> suffixes)
    {
        if (null == suffixes) return null;
        List<String> returnVal = new ArrayList<>();
        for (String suffix : suffixes)
        {
            returnVal.add(this.resourceUrl + suffix);
        }
        return returnVal;
    }

    /**
     * return choiceIndex's audio, fully qualified full path - no need to add URL prefix
     * ... returns null if there's no audio for the indicated choice
     */
    public List<String> getAudioPathsForChoice(int choiceIndex)
    {
        List<String> suffixes = this.responses.choices.get(choiceIndex).getAudioPaths();
        return this.addPrefixToSuffixes(suffixes);
    }

    /**
     * return stimIndex's audio, fully qualified full path - no need to add URL prefix
     * ... returns null if there's no audio for the indicated stimulus
     */
    public List<String> getAudioPathsForStimulus(int stimIndex)
    {
        List<String> suffixes = this.stimuli.get(stimIndex).getAudioPaths();
        return this.addPrefixToSuffixes(suffixes);
    }

    /**
     * return instructional audio, fully qualified full path - no need to add URL prefix
     * ... returns null if there's no audio for the instructions of this task
     */
    public List<String> getAudioPathsForInstructions()
    {
        List<String> suffixes = this.theInstructions.getAudioPaths();
        return this.addPrefixToSuffixes(suffixes);
    }

    @Override
    public String toString()
    {
        return "PlainTask{" +
                "level=" + this.level +
                ", theInstructions=" + this.theInstructions +
                '}';
    }
}
