package com.constanttherapy.share.tasks.types;

/**
 * This class contains the data required to display
 */
public class TaskTypeHierarchyNodeData {

	/**
	 * A system name that can also be used to reference a task type (slightly more readable)
	 */
	public String systemName;

	/**
	 * A more user-friendly way of displaying a task's name
	 */
	public String displayName;

	/**
	 * A description of this task type
	 */
	public String description;

	/**
	 * Indentation level for the node.  Top level nodes are 1, next level down are 2, etc.
	 */
	public int level = 0;

}
