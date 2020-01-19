package org.filkarchive;

import com.google.api.client.util.*;

import java.util.*;

abstract class FilkArchiveEntry
{
    final String user;
    final String time;
    final Map<String, List<String>> classifications = new HashMap<>();

    FilkArchiveEntry(ZooniverseClassificationEntry raw)
    {
        user = raw.fields.get("user_name");
        time = (String)raw.metadata.get("finished_at");

        for (Map<String, Object> annotation : raw.annotations) {
            String taskId = (String)annotation.get("task");

            Object value = annotation.get("value");
            if (Data.isNull(value))
            {
                classifications.put(taskId, new ArrayList<>());
            }
            else if (value instanceof List)
            {
                classifications.put(taskId, new ArrayList<String>((List<String>)value));
            }
            else if (value instanceof String) {
                ArrayList<String> item = new ArrayList<>(1);
                item.add((String)value);
                classifications.put(taskId, item);
            }
            else {
                System.err.printf("Surprising value %s of type %s%n",
                    value.toString(), value.getClass().toString());
            }
        }
    }

    abstract String getPrimaryKey();

    abstract String getSecondaryKey();
}
