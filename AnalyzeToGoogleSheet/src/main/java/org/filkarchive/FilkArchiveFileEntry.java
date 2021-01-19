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

    @Override
    String getSplitFileName()
    {
        return "Songbooks and Zines";
    }

    @Override
    String getSplitSheetName()
    {
        return source;
    }
}
