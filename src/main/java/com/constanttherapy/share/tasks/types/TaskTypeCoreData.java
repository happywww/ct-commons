package com.constanttherapy.share.tasks.types;

/**
 * This class contains the data that is used to display task type info
 * in the Instructions View
 */
public class TaskTypeCoreData extends TaskTypeHierarchyNodeData
{
    /**
     * The primary key of this task type from the database
     */
    public Integer typeId;
    /**
     * Therapy Task level (null for assessment tasks)
     */
    public Integer maxLevel;
    /**
     * Assessment Task Set (null for therapy tasks)
     */
    public Integer maxSet;
    /**
     * Path to sample image
     */
    public String sampleImagePath;
    /**
     * The path to the instructional video
     */
    public String instructionalVideoPath;
    /**
     * Tags associated with this task type
     */
    public String tags;
    /**
     * A shorter description for display on Task Info screen on client
     */
    public String descriptionLite;
    /**
     * A comma-separated list of deficits that this task helps with
     */
    public String helpsWith;
    /**
     *
     */
    public String iconPath;
}
