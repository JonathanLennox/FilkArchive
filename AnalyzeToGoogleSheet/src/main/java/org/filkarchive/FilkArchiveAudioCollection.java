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

    @Override
    public String getPrimaryKeyColumn()
    {
        return "file";
    }

    @Override
    public String getSecondaryKeyColumn()
    {
        return "cliptime";
    }

    private String trimClipTime(String cliptime)
    {
        int idx = cliptime.indexOf(' ');
        if (idx == -1)
        {
            return cliptime;
        }
        return cliptime.substring(0, idx);
    }

    @Override
    public int secondaryKeyComparator(String a, String b)
    {
        String clipstart1 = trimClipTime(a);
        String clipstart2 = trimClipTime(b);

        /* Sort by length first; then sort lexicographically within lengths.
          This will achieve a numeric sort.
         */
        if (clipstart1.length() != clipstart2.length())
        {
            return Integer.compare(clipstart1.length(), clipstart2.length());
        }
        return clipstart1.compareTo(clipstart2);
    }
}
