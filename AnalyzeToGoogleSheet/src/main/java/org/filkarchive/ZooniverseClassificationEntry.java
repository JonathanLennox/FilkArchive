package org.filkarchive;

import com.google.api.client.json.*;

import java.io.*;
import java.util.*;

class ZooniverseClassificationEntry
{
    Map<String, String> fields;

    GenericJson subjectDataSet, metadata;
    ArrayList<GenericJson> annotations;
    Map<String, String> subjectData;

    final JsonFactory factory;

    ZooniverseClassificationEntry(Map<String, String> f, JsonFactory factory)
        throws IOException
    {
        fields = f;
        this.factory = factory;

        subjectDataSet = readJsonField("subject_data", GenericJson.class);
        metadata = readJsonField("metadata", GenericJson.class);
        annotations =
            this.<ArrayList>readJsonField("annotations", ArrayList.class);

        if (subjectDataSet.size() == 0) {
            throw new IllegalArgumentException("No subjects found in classification");
        }
        if (subjectDataSet.size() > 1) {
            System.err.println("Classification has multiple subjects");
        }
        String subjectId = subjectDataSet.keySet().iterator().next();
        subjectData = (Map<String, String>)subjectDataSet.get(subjectId);
    }

    private <T> T readJsonField(String key, Class<T> clazz)
        throws IOException
    {
        if (!fields.containsKey(key))
            return null;

        JsonParser parser = factory.createJsonParser(fields.get(key));
        return parser.parse(clazz);
    }
}
