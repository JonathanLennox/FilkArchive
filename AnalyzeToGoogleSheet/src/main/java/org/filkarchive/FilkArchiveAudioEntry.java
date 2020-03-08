package org.filkarchive;

class FilkArchiveAudioEntry
    extends FilkArchiveEntry
{

    final String origfile;
    final String clipstart, cliptime;
    final String location, event;

    FilkArchiveAudioEntry(ZooniverseClassificationEntry raw)
    {
        super(raw);

        origfile = raw.subjectData.get("#origfile");

        clipstart = raw.subjectData.get("clipstart");
        cliptime = raw.subjectData.get("clipstart") + " - " + raw.subjectData.get("clipend");

        location = raw.subjectData.get("location");
        event = raw.subjectData.get("event");
    }

    @Override
    public String getColumnValue(String column)
    {
        switch (column)
        {
        case "file":
            return origfile;
        case "location":
            return location;
        case "event":
            return event;
        case "cliptime":
            return cliptime;
        }
        return super.getColumnValue(column);
    }
}
