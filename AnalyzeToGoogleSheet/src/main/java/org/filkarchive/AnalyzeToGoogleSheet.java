package org.filkarchive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.*;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.opencsv.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

public class AnalyzeToGoogleSheet
{
    private static final String APPLICATION_NAME = "Filk Archive Zooniverse Data Importer";
    private static final String CLASSIFICATIONS_DATA_FILE = "../filk-archive-classifications.csv";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = AnalyzeToGoogleSheet.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
										   HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
	    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
	    .setAccessType("offline")
	    .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Prints the names and majors of students in a sample spreadsheet:
     * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
     */
    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
        final String range = "Class Data!A2:E";
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
	    .setApplicationName(APPLICATION_NAME)
	    .build();
        ValueRange response = service.spreadsheets().values()
	    .get(spreadsheetId, range)
	    .execute();
        List<List<Object>> values = response.getValues();


/*        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            System.out.println("Name, Major");
            for (List row : values) {
                // Print columns A and E, which correspond to indices 0 and 4.
                System.out.printf("%s, %s\n", row.get(0), row.get(4));
            }
        }*/

        readClassifications(CLASSIFICATIONS_DATA_FILE);

        System.out.println("done");
    }

    static FilkArchiveCollection audioClassifications =
        new FilkArchiveCollection(FilkArchiveAudioEntry::new);

    static FilkArchiveCollection fileClassifications =
        new FilkArchiveCollection(FilkArchiveFileEntry::new);

    private static void readClassifications(String file)
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
                        new ZooniverseClassificationEntry(fields, JSON_FACTORY);

                    if (FilkArchiveAudioEntry.isInstance(classification))
                    {
                        audioClassifications.addEntry(classification);
                    }
                    else if (FilkArchiveFileEntry.isInstance(classification))
                    {
                        fileClassifications.addEntry(classification);
                    }
                    else
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
}
