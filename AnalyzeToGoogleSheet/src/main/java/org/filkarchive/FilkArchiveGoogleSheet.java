package org.filkarchive;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.extensions.java6.auth.oauth2.*;
import com.google.api.client.extensions.jetty.auth.oauth2.*;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.*;
import com.google.api.client.http.javanet.*;
import com.google.api.client.json.*;
import com.google.api.client.json.jackson2.*;
import com.google.api.client.util.store.*;
import com.google.api.services.sheets.v4.*;
import com.google.api.services.sheets.v4.model.*;
import org.eclipse.jetty.util.*;

import java.io.*;
import java.security.*;
import java.util.*;

public class FilkArchiveGoogleSheet
{
    static final String APPLICATION_NAME = "Filk Archive Zooniverse Data Importer";

    static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final String SPREADSHEET_ID = "1AX4U4e0kX_JcU2QKvBLyRxawrrBwj2jHlzZj39Qj9BA";

    Sheets service;

    Spreadsheet spreadsheet;

    FilkArchiveGoogleSheet() throws IOException, GeneralSecurityException
    {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, FilkArchiveGoogleSheet
            .getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build();

        refreshSheet();
    }

    /**
     * Global instance of the scopes required by this program.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String>
        SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = AnalyzeToGoogleSheet.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
										   HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
	    .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
	    .setAccessType("offline")
	    .build();
        LocalServerReceiver
            receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private void refreshSheet()
    {
        try
        {
            spreadsheet = service.spreadsheets().get(SPREADSHEET_ID).execute();
        }
        catch (Exception e)
        {
            System.err.printf("Error getting Google Sheet: %s%n", e.toString());
            e.printStackTrace(System.err);
        }
    }

    public int getOrAddSheet(String name) throws IOException
    {
        for (Sheet sheet: spreadsheet.getSheets())
        {
            if (sheet.getProperties().getTitle().equals(name))
            {
                return sheet.getProperties().getSheetId();
            }
        }

        return addSheet(name);
    }

    public int addSheet(String name) throws IOException
    {
        AddSheetRequest addSheetRequest = new AddSheetRequest();
        addSheetRequest.setProperties(new SheetProperties().setTitle(name));

        ArrayList<Request> requests = new ArrayList<>();

        requests.add(new Request().setAddSheet(addSheetRequest));

        BatchUpdateSpreadsheetRequest body
            = new BatchUpdateSpreadsheetRequest().setRequests(requests);

        BatchUpdateSpreadsheetResponse batchResponse =
            service.spreadsheets().batchUpdate(SPREADSHEET_ID, body).execute();

        AddSheetResponse response = batchResponse.getReplies().get(0).getAddSheet();

        refreshSheet();

        return response.getProperties().getSheetId();
    }
}
