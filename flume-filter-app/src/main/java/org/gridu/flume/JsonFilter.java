package org.gridu.flume;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter for a JSON array that removes square brackets and separators between JSONs
 */
public class JsonFilter implements Interceptor {

    public Event intercept(Event event){
        byte[] body= event.getBody();
        String eventBody = filter(new String(body));
        event.setBody(eventBody.getBytes());

        return event;
    }

    public List<Event> intercept(List<Event> events){
        return events.stream().map(this::intercept).collect(Collectors.toList());
    }

    /**
     * Filter for a JSON that removes square brackets
     * and separators between JSONs from array
     * @param body
     * @return
     */
    public String filter(String body){

        if (body == null || body.isEmpty())
            return body;

        char firstChar = body.charAt(0);
        char lastChar = body.charAt(body.length()-1);
        int firstIndex = 0;
        int lastIndex = body.length();

        if (firstChar == '[')
            firstIndex++;

        if (lastChar == ']' || lastChar == ',')
            lastIndex--;

        if(firstChar == '[' || lastChar == ']' || lastChar == ',')
            return body.substring(firstIndex, lastIndex);

        return body;
    }

    public void close() {

    }

    public void initialize(){}

    public static class Builder implements Interceptor.Builder {

        public void configure(Context context){}

        public Interceptor build() {
            return new JsonFilter();
        }
    }
}
