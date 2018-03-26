package com.constanttherapy.share.tasks.types;

/**
 * This class contains the data required to instantiate tasks
 * and render them on the client
 */
public class TaskTypeData extends TaskTypeCoreData
{

    /**
	 * The Default Factory Class name that should be used in creating tasks of this task type
	 */
	public transient String defaultFactoryClassName;

	/**
	 * The default Task class that the default factory will make instances of
	 */
	public transient String defaultTaskClassName;

    /**
	 * The default view class to use on the client-side (iPad app)
	 */
	public String defaultViewClassName;

    /**
	 * location of resources for this task type
	 */
	public String resourceUrl;

	/**
	 * The system name of this task type and all of its parent types in a 'namespace' format
	 */
	public String systemNamespace;

	/**
	 * The display name of this task type and all of its parent types in a 'namespace' format
	 */
	public String displayNamespace;

    /**
	 * The id of the task type this should be migrated to
	 */
	public Integer redirectId;

	/**
	 * The server version when this task became available
	 */
	public Float availableInVersion;

	/**
	 * The App version when this task became available on iOS
	 */
	public Float availableInVersion_iOS;

	/**
	 * The App version when this task became available on Android
	 */
	public Float availableInVersion_Android;
	/**
	 * maps to hw_assignable in task_types
	 * 1 = true, 0 = false
	 */
	public transient boolean canAssignAsHomework;
	/**
	 * For tasks that use speech recognition, this returns 1, otherwise 0.
	 */
	public transient boolean usesMicrophone;
}
