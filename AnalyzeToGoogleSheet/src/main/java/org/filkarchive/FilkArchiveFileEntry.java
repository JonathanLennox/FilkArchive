package org.filkarchive;

class FilkArchiveFileEntry
    extends FilkArchiveEntry
{
    static boolean isInstance(
        ZooniverseClassificationEntry raw)
    {
        return raw.subjectData.containsKey("source") &&
            raw.subjectData.containsKey("#file");
    }

    final String source, file;

    FilkArchiveFileEntry(ZooniverseClassificationEntry raw)
    {
        super(raw);

        source = raw.subjectData.get("source");
        file = raw.subjectData.get("#file");
    }

    @Override
    String getPrimaryKey()
    {
        return source;
    }

    @Override
    String getSecondaryKey()
    {
        return file;
    }
}
