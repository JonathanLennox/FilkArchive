package org.filkarchive;

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

    @Override
    public int compareTo(FilkArchiveEntryIndex o)
    {
        int cmp;

        if ((cmp = primaryKey.compareTo(o.primaryKey)) != 0)
            return cmp;

        if ((cmp = secondaryKey.compareTo(o.secondaryKey)) != 0)
            return cmp;

        return time.compareTo(o.time);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof FilkArchiveEntryIndex))
            return false;

        FilkArchiveEntryIndex i = (FilkArchiveEntryIndex)o;
        return primaryKey.equals(i.primaryKey) &&
            secondaryKey.equals(i.secondaryKey) &&
            time.equals(i.time);
    }

    @Override
    public String toString()
    {
        return primaryKey + "/" + secondaryKey + "@" + time;
    }
}
