package com.constanttherapy.tasks;

import com.constanttherapy.configuration.CTConfiguration;
import com.constanttherapy.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by madvani on 6/25/15.
 */
public class ScheduledTaskTypeLite
{
    public static final int MAX_COUNT_DEFAULT = 200;

    public Integer taskTypeId;
    /**
     * ScheduledTaskType Id
     */
    public Integer id;
    /**
     * References the containing schedule
     */
    public Integer scheduleId;
    /**
     * The number of tasks that should be created
     */
    public Integer taskCount;
    /**
     * The level of the tasks that will be created
     */
    public Integer taskLevel = null;
    /**
     * Additional settings & filters for this scheduledTaskType
     */
    private Map<String, String> parameters = null;
    /**
     * Represents the position of this schedule in the "queue" of schedules for
     * a patient
     */
    public Integer taskOrder;
    /**
     * System name for the task
     */
    public String systemName;

    transient public String domainName = null;

    static int maxTaskCount =
            CTConfiguration.getConfig("conf/ct-common-config.xml")
                    .getInteger("max-scheduled-task-type-task-count", MAX_COUNT_DEFAULT);

    ScheduledTaskTypeLite() {}

    ScheduledTaskTypeLite(int taskTypeId, int taskLevel, int taskCount)
    {
        this.taskTypeId = taskTypeId;
        this.taskLevel = taskLevel;
        this.taskCount = taskCount;
    }

    public Map<String, String> getParameters()
    {
        if (this.parameters == null)
            this.parameters = new HashMap<>();

        return this.parameters;
    }

    public void setParameters(Map<String, String> parameters)
    {
        this.parameters = parameters;
    }

    public void setParameters(String parametersJson)
    {
        this.parameters = GsonHelper.mapFromJson(parametersJson);
    }
}
