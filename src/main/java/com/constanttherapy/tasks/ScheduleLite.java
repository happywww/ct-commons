package com.constanttherapy.tasks;

import com.constanttherapy.util.GsonHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to generate a schedule JSON to send a new schedule from ReportService to the PatientService.
 *
 * TODO: Consider refactoring the Schedule and ScheduledTaskType classes in ct-services and extract interface for use here.
 * @author madvani
 *
 */
public class ScheduleLite
{
	public List<ScheduledTaskTypeLite> scheduledTaskTypes = new ArrayList<>();
	/**
	 * The primary key of this schedule in the database
	 */
	public Integer id;
	/**
	 * References the patient that this schedule is for
	 */
	public Integer patientId;
	/**
	 * If set to false, this schedule will be ignored
	 */
	public Boolean active;
	/**
	 * Clinician who assigned this schedule (NULL for schedules that were created by the patient himself/herself)
	 */
	public Integer clinicianId;
	/**
	 * Start date and time for the schedule
	 */
	public Timestamp startDate;
	/**
	 * End date and time for the schedule (NULL for schedules that have not been completed yet)
	 */
	public Timestamp endDate = null;
	public String type;  // bot or manual. Null --> manual

	public String description;

	protected ScheduleLite()
	{
	}

	public ScheduleLite(ResultSet rs) throws SQLException
	{
		this.id = rs.getInt("id");
		this.patientId = rs.getInt("patient_id");
		this.clinicianId = rs.getInt("clinician_id");
		this.active = rs.getBoolean("active");
		this.startDate = rs.getTimestamp("start_date");
		this.endDate = rs.getTimestamp("end_date");
		this.type = rs.getString("type");
		this.description = rs.getString("description");
	}

	public ScheduleLite(Integer patientId, Integer clinicianId)
	{
		this.patientId = patientId;
		this.clinicianId = clinicianId;
	}

	public void addScheduledTaskType(int taskTypeId, int taskLevel, int taskCount)
	{
		ScheduledTaskTypeLite sttl = new ScheduledTaskTypeLite(taskTypeId, taskLevel, taskCount);
        this.scheduledTaskTypes.add(sttl);
	}

	public String toJson()
	{
		return GsonHelper.toJson(this);
	}
}
