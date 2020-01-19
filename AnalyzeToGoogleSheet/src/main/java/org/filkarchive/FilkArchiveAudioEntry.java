package org.filkarchive;

class FilkArchiveAudioEntry
    extends FilkArchiveEntry
{
    static boolean isInstance(
        ZooniverseClassificationEntry raw)
    {
        return raw.subjectData.containsKey("#origfile");
    }

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
    String getPrimaryKey()
    {
        return origfile;
    }

    @Override
    String getSecondaryKey()
    {
        return clipstart;
    }
}
