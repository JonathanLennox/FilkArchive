package org.filkarchive;

class FilkArchiveFileEntry
    extends FilkArchiveEntry
{

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

    @Override
    public String getColumnValue(String column)
    {
        switch (column)
        {
        case "source":
            return source;
        case "file":
            return file;
        }
        return super.getColumnValue(column);
    }

}
