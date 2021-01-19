package org.filkarchive;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

abstract class FilkArchiveCollection
{
    final NavigableMap<FilkArchiveEntryIndex, FilkArchiveEntry> entries =
        new TreeMap<>();

    final TaskLabelMap taskLabels = new TaskLabelMap();

    abstract boolean isInstance(ZooniverseClassificationEntry classification);

    abstract FilkArchiveEntry getEntryFromClassification(
        ZooniverseClassificationEntry classification);

    public List<String> getColumns()
    {
        List<String> columns = new ArrayList<>();

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

    public abstract String getPrimaryKeyColumn();

    public abstract String getSecondaryKeyColumn();

    public abstract int secondaryKeyComparator(String a, String b);

    protected void addSpecificColumns(List<String> columns)
    {
    }

    public void addEntry(
        ZooniverseClassificationEntry classification)
    {
        FilkArchiveEntry entry = getEntryFromClassification(classification);

        addEntry(entry);

        taskLabels.updateTaskLabels(classification);
    }

    public void addEntry(FilkArchiveEntry entry)
    {
        FilkArchiveEntryIndex index = new FilkArchiveEntryIndex(
            entry.getColumnValue(getPrimaryKeyColumn()),
            entry.getColumnValue(getSecondaryKeyColumn()),
            entry.time,
            this::secondaryKeyComparator);

        if (entries.containsKey(index))
        {
            throw new IllegalArgumentException(
                "Duplicate entry with index " + index);
        }
        entries.put(index, entry);
    }

    public abstract String getCombinedSheetName();

    public void outputToSpreadsheet(FilkArchiveGoogleSheet googleSheet, String sheetName) throws
        IOException, InterruptedException
    {
        int sheetId = googleSheet.getOrAddSheet(sheetName);

        Map<String, Integer> columns = googleSheet.setColumns(sheetId, getColumns(), this::getColumnDescription);

        googleSheet.addNewRows(sheetId, entries, columns, getPrimaryKeyColumn(), getSecondaryKeyColumn(), this::secondaryKeyComparator);
    }

    public Collection<FilkArchiveCollection> splitByKey()
    {
        Map<String, FilkArchiveCollection> collections = new HashMap<>();

        for (Map.Entry<FilkArchiveEntryIndex, FilkArchiveEntry> entry: entries.entrySet())
        {
            if (!collections.containsKey(entry.getKey().primaryKey)) {
                try
                {
                    FilkArchiveCollection newCollection = this.getClass().getDeclaredConstructor().newInstance();
                    newCollection.taskLabels.copy(taskLabels);
                    collections.put(entry.getKey().primaryKey, newCollection);
                }
                catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e)
                {
                    System.err.println("Unable to instantiate new " + this.getClass().getName());
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            FilkArchiveCollection collection = collections.get(entry.getKey().primaryKey);
            collection.entries.put(entry.getKey(), entry.getValue());
        }

        return collections.values();
    }

    /* Only meaningful for split collections */
    public String getSplitFileName()
    {
        return entries.values().stream().findFirst().get().getSplitFileName();
    }

    /* Only meaningful for split collections */
    public String getSplitSheetName()
    {
        return entries.values().stream().findFirst().get().getSplitSheetName();
    }
}
