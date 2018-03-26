package com.constanttherapy.util;

import com.constanttherapy.util.CTTestBase;
import com.constanttherapy.util.GsonHelper;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by madvani on 4/18/16.
 */
public class GsonHelperTest extends CTTestBase
{
    @Test
    public void test_mapFromJson()
    {
        String json = "  { \"events\": [{ \"type\": \"instructionRepeat\", \"timestamp\": 1460924751 }, \n" +
                "{ \"index\": 2, \"type\": \"choiceSelected\", \"timestamp\": 1460924777 }] }\n";


        Map<String, Map<String, String>> test = GsonHelper.mapFromJson(json);

        System.out.println(test);


        ArrayList al = (ArrayList) test.get("events");

        for (Object map2: al)
        {

        }
    }

    @Test
    public void test_mapFromJson2()
    {
        String json = "  { \"events\": [{ \"type\": \"instructionRepeat\", \"timestamp\": 1460924751 }, \n" +
                "{ \"index\": 2, \"type\": \"choiceSelected\", \"timestamp\": 1460924777 }] }\n";

        AssessmentResponseEvents eventsContainer = GsonHelper.getGson().fromJson(json, AssessmentResponseEvents.class);

        System.out.println(eventsContainer);
    }

    /**
     * Represents a list of events
     */
    private class AssessmentResponseEvents
    {
        List<AssessmentResponseEvent> events;
    }

    /**
     * Represents an event of selecting an assessment choice
     */
    private class AssessmentResponseEvent
    {

        /**
         * Title of the selected choice
         */
        public String title;

        /**
         * Type of event
         */
        public String type;

        /**
         * The timestamp of the action
         */
        public Timestamp timestamp;

        /**
         * Selected option index
         */
        public Integer index;
    }
}
