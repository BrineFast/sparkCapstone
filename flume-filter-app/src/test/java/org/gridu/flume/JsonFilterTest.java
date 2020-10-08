package org.gridu.flume;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.flume.Event;
import org.apache.flume.event.JSONEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;


public class JsonFilterTest {

    private JsonFilter jsonFilter;

    @Before
    public void setUpJasonFilter(){
        jsonFilter = new JsonFilter();
    }

    public JsonArray createJsonArray(){
        JsonObject json1 = new JsonObject();
        json1.addProperty("type","click");
        json1.addProperty("ip","127.0.0.1");
        json1.addProperty("event_time","1500028835");
        json1.addProperty("url","https://blog.griddynamics.com/in-stream-processing-service-blueprint");

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(json1);

        return jsonArray;
    }

    @Test
    public void filterTest(){
        JsonArray jsonArray = createJsonArray();

        String json = jsonArray.get(0).toString();

        jsonFilter.filter(jsonArray.toString());

        Assert.assertEquals(json, jsonFilter.filter(json));
    }

    @Test
    public void interceptTest(){
        JsonArray jsonArray = createJsonArray();

        Event event = new JSONEvent();
        event.setBody(jsonArray.toString().getBytes());

        Assert.assertEquals(jsonFilter.filter(jsonArray.toString()),
                new String(jsonFilter.intercept(event).getBody(), StandardCharsets.UTF_8));

    }

}
