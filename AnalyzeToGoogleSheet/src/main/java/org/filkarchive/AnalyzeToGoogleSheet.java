package org.filkarchive;

import com.opencsv.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.*;
import java.util.*;

public class AnalyzeToGoogleSheet
{
    private static final String CLASSIFICATIONS_DATA_FILE = "../filk-archive-classifications.csv";

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.

        List<FilkArchiveCollection> collections = createCollections();

        readClassificationsToCollections(CLASSIFICATIONS_DATA_FILE, collections);

//        outputClassificationsToCombinedSpreadsheet(collections);

        outputClassificationsToSplitSpreadsheet(collections);

        System.out.println("done");
    }

    private static List<FilkArchiveCollection> createCollections()
    {
        List<FilkArchiveCollection> collections = new ArrayList<>();
        collections.add(new FilkArchiveAudioCollection());
        collections.add(new FilkArchiveFileCollection());

        return collections;
    }

    private static void readClassificationsToCollections(String file, List<FilkArchiveCollection> collections)
    {
        CSVReaderHeaderAware csvReader = null;
        try
        {
            FileReader fileReader = new FileReader(file);
            CSVReaderHeaderAwareBuilder builder =
                new CSVReaderHeaderAwareBuilder(fileReader);
            builder.withCSVParser(new RFC4180Parser());
            csvReader = builder.build();

            Map<String, String> fields;

            while ((fields = csvReader.readMap()) != null)
            {
                try
                {
                    ZooniverseClassificationEntry classification =
                        new ZooniverseClassificationEntry(fields, FilkArchiveGoogleSheet.JSON_FACTORY);

                    boolean found = false;
                    for (FilkArchiveCollection collection: collections)
                    {
                        if (collection.isInstance(classification))
                        {
                            collection.addEntry(classification);
                            found = true;
                            break;
                        }
                    }

                    if (!found)
                    {
                        System.err
                            .printf("Unrecognized classification at %s:%d%n",
                                file, csvReader.getLinesRead());
                    }
                }
                catch (Exception e)
                {
                    System.err.printf("Error at %s:%d: %s%n", file,
                        csvReader.getLinesRead(), e.toString());
                    e.printStackTrace(System.err);
                }
            }
        }
        catch (Exception e)
        {
            if (csvReader != null)
            {
                System.err.printf("Error at %s:%d: %s%n", file,
                    csvReader.getLinesRead(), e.toString());
            }
            else {
                System.err.printf("Error reading %s: %s%n", file, e.toString());
            }
            e.printStackTrace(System.err);
        }
    }

    static void outputClassificationsToCombinedSpreadsheet(
        List<FilkArchiveCollection> collections)
        throws IOException, InterruptedException, GeneralSecurityException
    {
        FilkArchiveGoogleSheet googleSheet = new FilkArchiveGoogleSheet();

        for (FilkArchiveCollection collection : collections)
        {
            String sheetName = collection.getCombinedSheetName();
            try
            {
                collection.outputToSpreadsheet(googleSheet, sheetName);
            }
            catch (Exception e)
            {
                System.err.printf("Error exporting %s: %s%n", sheetName, e.toString());
            }
        }
    }

    static void outputClassificationsToSplitSpreadsheet(
        List<FilkArchiveCollection> collections)
    {
        for (FilkArchiveCollection origCollection : collections)
        {
            Collection<FilkArchiveCollection> splitCollections = origCollection.splitByKey();

            for (FilkArchiveCollection collection: splitCollections)
            {
                String fileName = collection.getSplitFileName();
                String sheetName = collection.getSplitSheetName();
                try
                {
                    System.out.printf("Processing %s:%s\n", fileName, sheetName);
                    FilkArchiveGoogleSheet googleSheet = new FilkArchiveGoogleSheet(fileName);

                    collection.outputToSpreadsheet(googleSheet, sheetName);
                }
                catch (Exception e)
                {
                    System.err.printf("Error exporting %s:%s: %s%n", fileName, sheetName,
                        e.toString());
                    e.printStackTrace(System.err);
                }
            }
        }
    }

}
