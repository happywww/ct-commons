package com.constanttherapy.share.taskdata;

import com.constanttherapy.share.tasks.Instructions;

import java.util.List;

/**
 * A unit of therapy that can be presented to a User.
 * @author connormathews
 */
public abstract class TaskData
{

	public TaskData() {}

	/** All tasks must have a taskType, which references the "systemName" of the TaskType class */
	public String taskType;

	/** A single task id is used to reference tasks that are saved in the database (optional) */
	public Integer taskId;

	/**
	 * Session with which the task is associated.  This session Id is sent back in the
	 * response when user completes the task.
	 */
	public Integer sessionId;

	/**
	 * Number of questions sent for the item associated with this task.
	 * This is currently used by reading passage and voicemail tasks.
	 */
	public int itemQuestionCount = 1;

	/**
	 * Instructional text displayed to the user.
	 */
	public Instructions instruction = new Instructions();

	/**
	 * List of all resource names used by this task
	 */
	public List<String> resources;

	/**
	 * Location of resources for this task
	 */
	public String resourceUrl;

	/**
	 * ViewController used by iOS client to render task
	 */
	public String ios_ViewControllerClassName;

	@Override
	public String toString()
	{
		return "TaskData{" +
				"taskType='" + this.taskType + '\'' +
				", taskId=" + this.taskId +
				", resourceUrl='" + this.resourceUrl + '\'' +
				", instructions='" + this.instruction + '\'' +
				", sessionId=" + this.sessionId +
				'}';
	}
}
