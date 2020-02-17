package org.filkarchive;

import com.google.api.services.sheets.v4.*;
import com.google.api.services.sheets.v4.model.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

abstract class FilkArchiveCollection
{
    final NavigableMap<String, NavigableMap<String, List<FilkArchiveEntry>>> entries =
        new TreeMap<>();

    final TaskLabelMap taskLabels = new TaskLabelMap();

    abstract boolean isInstance(ZooniverseClassificationEntry classification);

    abstract FilkArchiveEntry getEntryFromClassification(ZooniverseClassificationEntry classification);

    public List<String> getColumns()
    {
        List<String> columns = new ArrayList<String>();

        columns.add("user");
        columns.add("time");
        addSpecificColumns(columns);

        columns.addAll(taskLabels.getTaskIds());

        return columns;
    }

    public String getColumnDescription(String label)
    {
        if (label.equals("user"))
        {
            return "Classifier";
        }
        else if (label.equals("time"))
        {
            return "Classification Time";
        }
        else if (taskLabels.taskLabels.containsKey(label))
        {
            return taskLabels.taskLabels.get(label);
        }
        else
        {
            throw new IllegalArgumentException("Unknown column label " + label);
        }
    }

    protected void addSpecificColumns(List<String> columns)
    {
    }

    public void addEntry(
        ZooniverseClassificationEntry classification)
    {
        FilkArchiveEntry entry = getEntryFromClassification(classification);

        NavigableMap<String, List<FilkArchiveEntry>> map1 =
            entries.computeIfAbsent(entry.getPrimaryKey(), k -> new TreeMap<>());

        List<FilkArchiveEntry> list =
            map1.computeIfAbsent(entry.getSecondaryKey(), k -> new ArrayList<>());

        list.add(entry);

        taskLabels.updateTaskLabels(classification);
    }

    public abstract String getSheetName();

    public void outputToSpreadsheet(FilkArchiveGoogleSheet googleSheet) throws
        IOException
    {
        int sheetId = googleSheet.getOrAddSheet(getSheetName());

        Map<String, Integer> columns = googleSheet.setColumns(sheetId, getColumns(), this::getColumnDescription);
    }
}
