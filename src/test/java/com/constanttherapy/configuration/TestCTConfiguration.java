package com.constanttherapy.configuration;
public class TestCTConfiguration
{

	
	/* Sample Config
	 
		 <memory-task-config>
		    <item-timeout>0.5</item-timeout>
		    
		    <tasks>
			    <task level="2">
			        <rows>3</rows>
			        <columns>4</columns>
			    </task>
			    <task level="3">
			        <rows>4</rows>
			        <columns>5</columns>
			    </task>
			    <task level="4">
			        <rows>4</rows>
			        <columns>6</columns>
			    </task>
			    <task level="5">
			        <rows>5</rows>
			        <columns>6</columns>
			    </task>	    
			    <task level="1">
			        <rows>2</rows>
			        <columns>3</columns>
			    </task>
		    </tasks>	  
		</memory-task-config>
	 
	 */
	/*
	@Test
	public void testMemoryTaskConfig()
	{
		Configuration config = CTConfiguration.getConfig("conf/ct-common-config.xml");
		// get item-timeout
		float timeout = config.getFloat("memory-task-config.item-timeout");
		assertTrue(timeout > 0.0);
		
		// get count of task elements
		Collection rows = config.getList("memory-task-config.tasks.task.rows");
		assertTrue(rows.size() == 5);
		
		
		ArrayList tasks = (ArrayList) config.getList("memory-task-config.tasks.task[@level]");
		
		// get columns for level 4
		//int cols = config.getInt("memory-task-config.tasks.task[@)
		int r = 0, c = 0;
		for (int i = 0; i < tasks.size(); i++)
		{
			int val = Integer.parseInt((String)tasks.get(i));
			if (val == 4)
			{
				r = config.getInt("memory-task-config.tasks.task(" + i + ").rows");
				c = config.getInt("memory-task-config.tasks.task(" + i + ").columns");
				break;
			}
		}
		
		assertTrue(r == 4);
		assertTrue(c == 6);
	}
	
	*/
}
