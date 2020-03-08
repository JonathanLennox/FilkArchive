package org.filkarchive;

import java.util.*;

public class FilkArchiveFileCollection
    extends FilkArchiveCollection
{
    boolean isInstance(ZooniverseClassificationEntry classification)
    {
        return classification.subjectData.containsKey("source") &&
            classification.subjectData.containsKey("#file");
    }

    @Override
    FilkArchiveEntry getEntryFromClassification(
        ZooniverseClassificationEntry classification)
    {
        return new FilkArchiveFileEntry(classification);
    }

    @Override
    protected void addSpecificColumns(List<String> columns)
    {
        final String[] toAdd = {"source", "file"};
        columns.addAll(Arrays.asList(toAdd));
    }

    @Override
    public String getSheetName()
    {
        return "File";
    }

    @Override
    public String getColumnDescription(String label)
    {
        switch (label)
        {
        case "source":
            return "Source";
        case "file":
            return "File";
        default:
            return super.getColumnDescription(label);
        }
    }

    @Override
    public String getPrimaryKeyColumn()
    {
        return "source";
    }

    @Override
    public String getSecondaryKeyColumn()
    {
        return "file";
    }

    @Override
    public int secondaryKeyComparator(String a, String b)
    {
        return a.compareTo(b);
    }
}
