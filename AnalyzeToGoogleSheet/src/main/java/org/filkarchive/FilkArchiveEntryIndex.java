package org.filkarchive;

import java.util.*;

public class FilkArchiveEntryIndex implements Comparable<FilkArchiveEntryIndex>
{
    public String primaryKey;
    public String secondaryKey;
    public String time;

    public FilkArchiveEntryIndex(String primaryKey, String secondaryKey,
        String time, Comparator<String> secondaryKeyComparator)
    {
        this.primaryKey = primaryKey;
        this.secondaryKey = secondaryKey;
        this.time = time;

        this.secondaryComp = Comparator.nullsFirst(secondaryKeyComparator);
    }

    private Comparator<String> secondaryComp;
    private static Comparator<String> timeComp = Comparator.nullsFirst(String::compareTo);

    @Override
    public int compareTo(FilkArchiveEntryIndex o)
    {
        int cmp;

        if ((cmp = primaryKey.compareTo(o.primaryKey)) != 0)
            return cmp;

        if ((cmp = secondaryComp.compare(secondaryKey, o.secondaryKey)) != 0)
            return cmp;

        return timeComp.compare(time, o.time);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof FilkArchiveEntryIndex))
            return false;

        FilkArchiveEntryIndex i = (FilkArchiveEntryIndex)o;
        return primaryKey.equals(i.primaryKey) &&
            secondaryComp.compare(secondaryKey, i.secondaryKey) == 0 &&
            timeComp.compare(time, i.time) == 0;
    }

    @Override
    public String toString()
    {
        if (secondaryKey == null && time == null)
        {
            return primaryKey + " -HEADER-";
        }
        return primaryKey + "/" + secondaryKey + "@" + time;
    }
}
