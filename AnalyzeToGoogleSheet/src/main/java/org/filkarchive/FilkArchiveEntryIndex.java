package org.filkarchive;

import java.util.*;

public class FilkArchiveEntryIndex implements Comparable<FilkArchiveEntryIndex>
{
    public String primaryKey;
    public String secondaryKey;
    public String time;

    public FilkArchiveEntryIndex(String primaryKey, String secondaryKey,
        String time)
    {
        this.primaryKey = primaryKey;
        this.secondaryKey = secondaryKey;
        this.time = time;
    }

    private static Comparator<String> comp = Comparator.nullsFirst(String::compareTo);

    @Override
    public int compareTo(FilkArchiveEntryIndex o)
    {
        int cmp;

        if ((cmp = primaryKey.compareTo(o.primaryKey)) != 0)
            return cmp;

        if ((cmp = comp.compare(secondaryKey, o.secondaryKey)) != 0)
            return cmp;

        return comp.compare(time, o.time);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof FilkArchiveEntryIndex))
            return false;

        FilkArchiveEntryIndex i = (FilkArchiveEntryIndex)o;
        return primaryKey.equals(i.primaryKey) &&
            comp.compare(secondaryKey, i.secondaryKey) == 0 &&
            comp.compare(time, i.time) == 0;
    }

    @Override
    public String toString()
    {
        if (secondaryKey == null && time == null)
        {
            return "primaryKey -HEADER-";
        }
        return primaryKey + "/" + secondaryKey + "@" + time;
    }
}
