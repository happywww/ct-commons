package com.constanttherapy.share.taskdata;

import java.sql.Timestamp;
import java.util.List;

/**
 * A collection of BaseTask objects completed in one period of time.
 * @author connormathews
 */
public class SessionData
{
	/**
	 * Maps to session type.
	 */
	public enum SessionTypes
	{
		// TODO: [mahendra, 3/2/16 7:35 PM] - replace with SessionTypes from ct-service
		ADHOC, ASSISTED, SCHEDULED
	}

	/**
	 * Baseline.
	 */
	public boolean isBaseline;

	/**
	 * Unique id associated with current BaseSession.
	 */
	public Integer id;

	/**
	 * Type of session: adhoc, assisted, scheduled.
	 */
	public SessionTypes type;

	/**
	 * References User associated with this BaseSession.
	 */
	public Integer userId;

	/**
	 * Date and time for when this BaseSession begins.
	 */
	public Timestamp startTime;

	/**
	 * Difficulty level associated this session.
	 */
	public Integer level;

	/**
	 * Number of tasks completed in this session.
	 */
	public Integer completedTaskCount;

	/**
	 * Total number of tasks in this session.
	 */
	public Integer totalTaskCount;

	/**
	 * Collection of BaseTask objects.
	 */
	public List<TaskData> tasks;

	/** IDs of all subsessions - not just the ones for which tasks are included
	 * if indeed tasks are included */
	public List<Integer> subsessionIds = null;

	@Override
	public String toString()
	{
		return "SessionData{" +
				"id=" + this.id +
				", type=" + this.type +
				", userId=" + this.userId +
				", startTime=" + this.startTime +
				", level=" + this.level +
				", completedTaskCount=" + this.completedTaskCount +
				", totalTaskCount=" + this.totalTaskCount +
				'}';
	}
}
