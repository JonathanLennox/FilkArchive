package org.filkarchive;

import java.util.*;

public class FilkArchiveAudioCollection extends FilkArchiveCollection
{
    boolean isInstance(ZooniverseClassificationEntry classification)
    {
        return classification.subjectData.containsKey("#origfile");
    }

    @Override
    FilkArchiveEntry getEntryFromClassification(
        ZooniverseClassificationEntry classification)
    {
        return new FilkArchiveAudioEntry(classification);
    }

    @Override
    protected void addSpecificColumns(List<String> columns)
    {
        final String[] toAdd = {"file", "location", "event", "cliptime"};
        columns.addAll(Arrays.asList(toAdd));
    }

    @Override
    public String getSheetName()
    {
        return "Audio";
    }

    @Override
    public String getColumnDescription(String label)
    {
        switch (label)
        {
        case "file":
            return "Original File";
        case "location":
            return "Location";
        case "event":
            return "Event";
        case "cliptime":
            return "Clip Time";
        default:
            return super.getColumnDescription(label);
        }
    }


}
