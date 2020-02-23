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

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.function.*;

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
        body.setIncludeSpreadsheetInResponse(true);

        BatchUpdateSpreadsheetResponse batchResponse =
            service.spreadsheets().batchUpdate(SPREADSHEET_ID, body).execute();

        spreadsheet = batchResponse.getUpdatedSpreadsheet();

        AddSheetResponse response = batchResponse.getReplies().get(0).getAddSheet();

        return response.getProperties().getSheetId();
    }

    private SheetProperties getSheetProperties(int sheetId)
    {
        for (Sheet sheet: spreadsheet.getSheets())
        {
            if (sheet.getProperties().getSheetId() == sheetId)
            {
                return sheet.getProperties();
            }
        }

        throw new IllegalStateException();
    }

    private static String toColumn(int column)
    {
        if (column <= 0) {
            throw new IllegalArgumentException("Bad column value " + column);
        }
        StringBuilder builder = new StringBuilder();

        while (column > 0)
        {
            int rem = column % 26;
            column = column / 26 - 1;
            if (rem == 0)
            {
                builder.append('Z');
            }
            else
            {
                builder.append((char)(rem - 1 + 'A'));
            }
        }
        return builder.reverse().toString();
    }

    private static String coordinatesToRange(SheetProperties sheetProperties, int startColumn, int startRow, int endColumn, int endRow)
    {
        StringBuilder builder = new StringBuilder(sheetProperties.getTitle()).append('!');

        builder.append(toColumn(startColumn)).append(startRow);

        if (startRow != endRow || startColumn != endColumn)
        {
            builder.append(":");
            builder.append(toColumn(endColumn)).append(endRow);
        }

        return builder.toString();
    }

    private List<Object> getColumnHeaders(int sheetId)
        throws IOException
    {
        SheetProperties sheetProperties = getSheetProperties(sheetId);

        String headersRange = coordinatesToRange(sheetProperties, 1, 1, sheetProperties.getGridProperties().getColumnCount(), 1);

        ValueRange response = service.spreadsheets().values().get(SPREADSHEET_ID, headersRange).execute();

        if (response.getValues() == null || response.getValues().size() == 0)
        {
            return Collections.emptyList();
        }
        return response.getValues().get(0);
    }

    private Map<String, Integer> getColumns(int sheetId)
        throws IOException
    {
        Map<String, Integer> columnLocations = new HashMap<>();

        SearchDeveloperMetadataRequest metadataSearchRequest = new SearchDeveloperMetadataRequest();
        metadataSearchRequest.setDataFilters(Collections.singletonList(new DataFilter().
            setDeveloperMetadataLookup(new DeveloperMetadataLookup().setMetadataKey("columnId"))));

        SearchDeveloperMetadataResponse metadataSearchResult = service.spreadsheets().developerMetadata().search(SPREADSHEET_ID, metadataSearchRequest).execute();

        if (metadataSearchResult.getMatchedDeveloperMetadata() != null)
        {
            for (MatchedDeveloperMetadata metadata : metadataSearchResult.getMatchedDeveloperMetadata())
            {
                DeveloperMetadata data = metadata.getDeveloperMetadata();
                if (data.getLocation().getDimensionRange().getSheetId() != sheetId)
                {
                    continue;
                }
                if (!data.getLocation().getLocationType().equals("COLUMN"))
                {
                    throw new RuntimeException("Surprising metadata data location " + data.getLocation().getLocationType() +
                        "for " + data.getMetadataKey() + "=" + data.getMetadataValue());
                }
                columnLocations.put(data.getMetadataValue(), data.getLocation().getDimensionRange().getStartIndex());
            }
        }

        return columnLocations;
    }

    public Map<String, Integer> setColumns(int sheetId, List<String> columnValues, Function<String, String> describe)
        throws IOException
    {
        int i;

        List<Object> columnHeaders = getColumnHeaders(sheetId);

        Map<String, Integer> columnLocations = getColumns(sheetId);

        for (i = 0; i < columnValues.size(); i++)
        {
            ArrayList<Request> requests = new ArrayList<>();

            String columnId = columnValues.get(i);
            String description = describe.apply(columnId);

            if (columnLocations.containsKey(columnId))
            {
                int loc = columnLocations.get(columnId);
                if (columnHeaders.get(loc).equals(description))
                {
                    continue;
                }
                UpdateCellsRequest update = new UpdateCellsRequest().setRows(Collections.singletonList(
                    new RowData().setValues(Collections.singletonList(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(description)))))).
                    setFields("*").
                    setStart(new GridCoordinate().setColumnIndex(loc).setRowIndex(0).setSheetId(sheetId));
                requests.add(new Request().setUpdateCells(update));
            }
            else
            {
                int loc;
                if (i == 0)
                {
                    loc = 0;
                }
                else
                {
                    String prevColumnId = columnValues.get(i-1);
                    if (!columnLocations.containsKey(prevColumnId))
                    {
                        throw new RuntimeException("columnLocations does not contain column ID " + (i-1) +
                            " " + prevColumnId + " when processing column ID " + i + " " + columnId);
                    }
                    loc = columnLocations.get(prevColumnId) + 1;
                }
                InsertDimensionRequest insertDimension = new InsertDimensionRequest().setInheritFromBefore(i > 0).
                    setRange(new DimensionRange().setSheetId(sheetId).setDimension("COLUMNS").setStartIndex(loc).setEndIndex(loc+1));
                requests.add(new Request().setInsertDimension(insertDimension));

                CreateDeveloperMetadataRequest createMetadata = new CreateDeveloperMetadataRequest().setDeveloperMetadata(new DeveloperMetadata().
                    setMetadataKey("columnId").setMetadataValue(columnId).setLocation(new DeveloperMetadataLocation().
                    setDimensionRange(new DimensionRange().setSheetId(sheetId).setDimension("COLUMNS").setStartIndex(loc).setEndIndex(loc+1))).setVisibility("DOCUMENT"));
                requests.add(new Request().setCreateDeveloperMetadata(createMetadata));

                UpdateCellsRequest update = new UpdateCellsRequest().setRows(Collections.singletonList(
                    new RowData().setValues(Collections.singletonList(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(description)))))).
                    setFields("*").
                    setStart(new GridCoordinate().setColumnIndex(loc).setRowIndex(0).setSheetId(sheetId));
                requests.add(new Request().setUpdateCells(update));
            }

            BatchUpdateSpreadsheetRequest body
                = new BatchUpdateSpreadsheetRequest().setRequests(requests);
            body.setIncludeSpreadsheetInResponse(true);

            BatchUpdateSpreadsheetResponse batchResponse =
                service.spreadsheets().batchUpdate(SPREADSHEET_ID, body).execute();

            spreadsheet = batchResponse.getUpdatedSpreadsheet();

            columnHeaders = getColumnHeaders(sheetId);

            columnLocations = getColumns(sheetId);
        }

        return columnLocations;
    }
}
