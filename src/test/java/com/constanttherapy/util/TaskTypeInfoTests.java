package com.constanttherapy.util;

import com.constanttherapy.enums.ClientPlatform;
import com.constanttherapy.tasks.TaskTypeInfo;
import com.constanttherapy.util.CTTestBase;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertTrue;

/**
 * Created by Mahendra on 5/4/2015.
 */
public class TaskTypeInfoTests extends CTTestBase
{
    /*

    Generate test cases for the following scenario
    +-----+-------------+-------------+----------------------+--------------------------+------------------------------+
    | id  | redirect_id | system_name | available_in_version | available_in_version_ios | available_in_version_android |
    +-----+-------------+-------------+----------------------+--------------------------+------------------------------+
    | 101 |         102 | task_v1     |                    2 |                        2 |                         NULL |
    | 102 |         103 | task_v2     |                  2.3 |                      2.3 |                         NULL |
    | 103 |         104 | task_v3     |                  2.5 |                      2.5 |                          2.5 |
    | 104 |        NULL | task_v4     |                  2.7 |                      2.7 |                         NULL |
    +-----+-------------+-------------+----------------------+--------------------------+------------------------------+
    */

    @Test
    public void test_getAvailableTaskType()
    {
        TaskTypeInfo tti = TaskTypeInfo.getAvailableTaskType(101, 2.0f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("task_v1"));

        // Android client v2.0 doesn't exist, so return null
        tti = TaskTypeInfo.getAvailableTaskType(101, 2.0f, ClientPlatform.Android);
        assertTrue(tti == null);

        // task_v1 assigned, redirect to task_v2
        tti = TaskTypeInfo.getAvailableTaskType(101, 2.3f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("task_v2"));

        // Android client v2.3 doesn't exist, so return null
        tti = TaskTypeInfo.getAvailableTaskType(101, 2.3f, ClientPlatform.Android);
        assertTrue(tti == null);

        //  task_v1 assigned, redirect to task_v3
        tti = TaskTypeInfo.getAvailableTaskType(101, 2.5f, ClientPlatform.Android);
        assertTrue(tti.systemName.equals("task_v3"));

        //  task_v2 assigned, redirect to task_v3
        tti = TaskTypeInfo.getAvailableTaskType(102, 2.5f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("task_v3"));

        //  task_v2 assigned, redirect to task_v3
        tti = TaskTypeInfo.getAvailableTaskType(102, 2.5f, ClientPlatform.Android);
        assertTrue(tti.systemName.equals("task_v3"));

        //  task_v2 assigned, redirect to task_v4
        tti = TaskTypeInfo.getAvailableTaskType(102, 2.7f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("task_v4"));

        //  task_v2 assigned, redirect to task_v3 because task_v4 not implemented on Android
        tti = TaskTypeInfo.getAvailableTaskType(102, 2.7f, ClientPlatform.Android);
        assertTrue(tti.systemName.equals("task_v3"));

        //  task_v4 assigned, redirect to task_v1
        tti = TaskTypeInfo.getAvailableTaskType(104, 2.0f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("task_v1"));

        //  task_v4 assigned, redirect to task_v2
        tti = TaskTypeInfo.getAvailableTaskType(104, 2.3f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("task_v2"));

        //  task_v4 assigned, redirect to task_v3
        tti = TaskTypeInfo.getAvailableTaskType(104, 2.5f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("task_v3"));

        //  task_v4 assigned, and is implemented in the 2.7 iOS client
        tti = TaskTypeInfo.getAvailableTaskType(104, 2.7f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("task_v4"));
    }

    @Test
    public void test_getAvailableTaskType2()
    {
        TaskTypeInfo tti = TaskTypeInfo.getAvailableTaskType(18, 2.0f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("reading_passage_tasks"));

        tti = TaskTypeInfo.getAvailableTaskType(18, 2.7f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("reading_passage_tasks"));

        tti = TaskTypeInfo.getAvailableTaskType(18, 2.8f, ClientPlatform.iOS);
        assertTrue(tti.systemName.equals("short_reading_tasks"));
    }

    @Test
    public void test_findHighestVersionTaskType()
    {
        TaskTypeInfo.init();
        TaskTypeInfo tti = TaskTypeInfo.findHighestVersionTaskType("naming_picture_tasks");
        System.out.println(tti.systemName);
    }

    @Test
    public void test_getAllTaskVersions()
    {
        TaskTypeInfo.init();
        Collection<String> taskSysNames1 = TaskTypeInfo.getAllTaskVersions("word_reading_tasks");
        assertTrue(taskSysNames1.size() == 4);

        Collection<String> taskSysNames2 = TaskTypeInfo.getAllTaskVersions("word_reading_tasks_v2");
        assertTrue(taskSysNames2.size() == 4);

        Collection<String> taskSysNames3 = TaskTypeInfo.getAllTaskVersions("word_reading_tasks_v3");
        assertTrue(taskSysNames3.size() == 4);
    }
}
