package org.filkarchive;

import java.util.*;
import java.util.function.*;

class FilkArchiveCollection
{
    final NavigableMap<String, NavigableMap<String, List<FilkArchiveEntry>>> entries =
        new TreeMap<>();

    final TaskLabelMap
        taskLabels = new TaskLabelMap();

    final Function<ZooniverseClassificationEntry, FilkArchiveEntry>
        constructor;

    FilkArchiveCollection(Function<ZooniverseClassificationEntry, FilkArchiveEntry> constructor)
    {
        this.constructor = constructor;
    }

    public void addEntry(
        ZooniverseClassificationEntry classification)
    {
        FilkArchiveEntry entry = constructor.apply(classification);

        NavigableMap<String, List<FilkArchiveEntry>> map1 =
            entries.computeIfAbsent(entry.getPrimaryKey(), k -> new TreeMap<>());

        List<FilkArchiveEntry> list =
            map1.computeIfAbsent(entry.getSecondaryKey(), k -> new ArrayList<>());

        list.add(entry);

        taskLabels.updateTaskLabels(classification);
    }
}
