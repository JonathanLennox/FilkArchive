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
import com.google.api.services.drive.model.*;
import com.google.api.services.sheets.v4.*;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.drive.*;

import java.io.*;
import java.io.File;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

public class FilkArchiveGoogleSheet
{
    static final String APPLICATION_NAME = "Filk Archive Zooniverse Data Importer";

    static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final String SINGLE_SPREADSHEET_ID = "1AX4U4e0kX_JcU2QKvBLyRxawrrBwj2jHlzZj39Qj9BA";

    private static final String PARENT_FOLDER_ID = "1wpl58oQe1ONEa4hCCY2PsEYTcDXDgjtS";

    /* Is this defined somewhere in the sheets API? */
    static final String GOOGLE_SHEETS_MIME_TYPE = "application/vnd.google-apps.spreadsheet";

    Sheets sheetsService;

    Spreadsheet spreadsheet;

    private final String spreadsheetId;

    FilkArchiveGoogleSheet() throws IOException, InterruptedException, GeneralSecurityException
    {
        spreadsheetId = SINGLE_SPREADSHEET_ID;

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY,
            getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build();

        refreshSheet();
    }

    FilkArchiveGoogleSheet(String name) throws IOException, InterruptedException, GeneralSecurityException
    {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Credential credentials = getCredentials(HTTP_TRANSPORT);

        sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
            .setApplicationName(APPLICATION_NAME)
            .build();

        Drive driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
            .setApplicationName(APPLICATION_NAME)
            .build();

        String query = "name='" + name + "'"
            + " and mimeType='" + GOOGLE_SHEETS_MIME_TYPE + "'"
            + " and '" + PARENT_FOLDER_ID + "' in parents";

        String pageToken = null;

        FileList files = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("nextPageToken, files(id,name)")
            .setPageToken(pageToken)
            .execute();

        if (files.getFiles().isEmpty()) {
            com.google.api.services.drive.model.File fileRequest =
                new com.google.api.services.drive.model.File()
                .setMimeType(GOOGLE_SHEETS_MIME_TYPE)
                .setName(name)
                .setParents(Collections.singletonList(PARENT_FOLDER_ID));

            com.google.api.services.drive.model.File newFile =
                driveService.files().create(fileRequest).execute();

            spreadsheetId = newFile.getId();
        }
        else {
            if (files.getFiles().size() > 1) {
                System.out.println("Warning: more than one file found with name " + name);
            }
            spreadsheetId = files.getFiles().get(0).getId();
        }

        refreshSheet();
    }


    /**
     * Global instance of the scopes required by this program.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String>
        SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE);

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
        throws IOException, InterruptedException
    {
        spreadsheet =
            ExponentialBackoff.execute( () ->
                sheetsService.spreadsheets().get(spreadsheetId).execute()
            );
    }

    public int getOrAddSheet(String name) throws IOException, InterruptedException
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

    public int addSheet(String name) throws IOException, InterruptedException
    {
        AddSheetRequest addSheetRequest = new AddSheetRequest();
        addSheetRequest.setProperties(new SheetProperties().setTitle(name));

        ArrayList<Request> requests = new ArrayList<>();

        requests.add(new Request().setAddSheet(addSheetRequest));

        BatchUpdateSpreadsheetRequest body
            = new BatchUpdateSpreadsheetRequest().setRequests(requests);
        body.setIncludeSpreadsheetInResponse(true);

        BatchUpdateSpreadsheetResponse batchResponse =
            ExponentialBackoff.execute( () ->
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, body).execute()
            );

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
        int column1 = column + 1;

        if (column1 <= 0) {
            throw new IllegalArgumentException("Bad column value " + column1);
        }
        StringBuilder builder = new StringBuilder();

        while (column1 > 0)
        {
            int rem = column1 % 26;
            column1 = column1 / 26;
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

    /**
     * Convert an indexed range (0-based) to A1 range notation.
     */
    private static String coordinatesToRange(SheetProperties sheetProperties, int startColumn, int startRow, int endColumn, int endRow)
    {
        StringBuilder builder = new StringBuilder(sheetProperties.getTitle()).append('!');

        builder.append(toColumn(startColumn)).append(startRow+1);

        if (startRow != endRow || startColumn != endColumn)
        {
            builder.append(":");
            builder.append(toColumn(endColumn)).append(endRow+1);
        }

        return builder.toString();
    }

    private List<Object> getColumnHeaders(int sheetId)
        throws IOException, InterruptedException
    {
        SheetProperties sheetProperties = getSheetProperties(sheetId);

        String headersRange = coordinatesToRange(sheetProperties, 0, 0, sheetProperties.getGridProperties().getColumnCount() - 1, 0);

        ValueRange response =
            ExponentialBackoff.execute( () ->
                sheetsService.spreadsheets().values().get(
                    spreadsheetId, headersRange).execute()
            );

        if (response.getValues() == null || response.getValues().size() == 0)
        {
            return Collections.emptyList();
        }
        return response.getValues().get(0);
    }

    private Map<String, Integer> getColumns(int sheetId)
        throws IOException, InterruptedException
    {
        Map<String, Integer> columnLocations = new HashMap<>();

        SearchDeveloperMetadataRequest metadataSearchRequest = new SearchDeveloperMetadataRequest();
        metadataSearchRequest.setDataFilters(Collections.singletonList(new DataFilter().
            setDeveloperMetadataLookup(new DeveloperMetadataLookup().setMetadataKey("columnId"))));

        SearchDeveloperMetadataResponse metadataSearchResult =
            ExponentialBackoff.execute( () ->
                sheetsService.spreadsheets().developerMetadata().search(
                    spreadsheetId, metadataSearchRequest).execute()
            );

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
        throws IOException, InterruptedException
    {
        int i;

        List<Object> columnHeaders = getColumnHeaders(sheetId);

        Map<String, Integer> columnLocations = getColumns(sheetId);

        String prevColumnProcessed = null;
        int prevColumnProcessedId = -1;

        ArrayList<Request> requests = new ArrayList<>();

        for (i = 0; i < columnValues.size(); i++)
        {
            String columnId = columnValues.get(i);
            String description = describe.apply(columnId);
            int loc;

            if (columnLocations.containsKey(columnId))
            {
                loc = columnLocations.get(columnId);
                if (loc < columnHeaders.size() && columnHeaders.get(loc).equals(description))
                {
                    continue;
                }
                CellFormat format = new CellFormat().setTextFormat(new TextFormat().setBold(true));
                format.setWrapStrategy("WRAP");
                format.setVerticalAlignment("TOP");
                CellData cell = new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(description));
                cell.setUserEnteredFormat(format);
                UpdateCellsRequest update = new UpdateCellsRequest().setRows(Collections.singletonList(
                    new RowData().setValues(Collections.singletonList(cell)))).
                    setFields("*").
                    setStart(new GridCoordinate().setColumnIndex(loc).setRowIndex(0).setSheetId(sheetId));
                requests.add(new Request().setUpdateCells(update));
            }
            else
            {
                if (i == 0)
                {
                    loc = 0;
                }
                else
                {
                    String prevColumnId = columnValues.get(i-1);
                    if (Objects.equals(prevColumnId, prevColumnProcessed))
                    {
                        loc = prevColumnProcessedId + 1;
                    }
                    else
                    {
                        if (!requests.isEmpty())
                        {
                            BatchUpdateSpreadsheetRequest body
                                = new BatchUpdateSpreadsheetRequest()
                                .setRequests(requests);

                            ExponentialBackoff.execute( () ->
                                sheetsService.spreadsheets()
                                    .batchUpdate(spreadsheetId, body).execute()
                            );

                            columnHeaders = getColumnHeaders(sheetId);
                            columnLocations = getColumns(sheetId);

                            requests = new ArrayList<>();
                        }

                        if (!columnLocations.containsKey(prevColumnId))
                        {
                            throw new RuntimeException(
                                "columnLocations does not contain column ID "
                                    + (i - 1) +
                                    " " + prevColumnId
                                    + " when processing column ID " + i + " "
                                    + columnId);
                        }
                        loc = columnLocations.get(prevColumnId) + 1;
                    }
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
            prevColumnProcessed = columnId;
            prevColumnProcessedId = loc;
        }
        if (!requests.isEmpty())
        {
            BatchUpdateSpreadsheetRequest body
                = new BatchUpdateSpreadsheetRequest().setRequests(requests);

            ExponentialBackoff.execute( () ->
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, body)
                    .execute()
            );
            columnLocations = getColumns(sheetId);
        }

        return columnLocations;
    }

    private int numRows(GridData data)
    {
        if (data.getRowData() == null)
            return 0;
        return data.getRowData().size();
    }

    private String getUserEnteredValue(GridData data, int row, int column)
    {
        if (data.getRowData() == null)
            return null;
        if (row >= data.getRowData().size())
            return null;
        RowData rowData = data.getRowData().get(row);
        if (rowData == null || rowData.getValues() == null)
            return null;
        if (column >= rowData.getValues().size())
            return null;
        CellData cell = rowData.getValues().get(column);
        if (cell.getUserEnteredValue() == null)
            return null;
        return cell.getUserEnteredValue().getStringValue();
    }

    private List<FilkArchiveEntryIndex> getRows(int sheetId, int primaryKeyColumn, int secondaryKeyColumn, int timeColumn, Comparator<String> secondaryKeyComparator)
        throws IOException, InterruptedException
    {
        SheetProperties sheetProperties = getSheetProperties(sheetId);

        ArrayList<String> ranges = new ArrayList<>();

        ranges.add(coordinatesToRange(sheetProperties, primaryKeyColumn, 1, primaryKeyColumn, sheetProperties.getGridProperties().getRowCount()));
        ranges.add(coordinatesToRange(sheetProperties, secondaryKeyColumn, 1, secondaryKeyColumn, sheetProperties.getGridProperties().getRowCount()));
        ranges.add(coordinatesToRange(sheetProperties, timeColumn, 1, timeColumn, sheetProperties.getGridProperties().getRowCount()));

        Spreadsheet response =
            ExponentialBackoff.execute( () ->
                sheetsService.spreadsheets().get(spreadsheetId).setRanges(ranges).
                    setFields("sheets.data.rowData.values.userEnteredValue,sheets.data.rowMetadata.developerMetadata").
                    execute()
            );

        GridData primaryKeyData   = response.getSheets().get(0).getData().get(0);
        GridData secondaryKeyData = response.getSheets().get(0).getData().get(1);
        GridData timeData         = response.getSheets().get(0).getData().get(2);

        int minSize = Math.min(numRows(primaryKeyData), Math.min(numRows(secondaryKeyData), numRows(timeData)));
        List<FilkArchiveEntryIndex> rows = new ArrayList<>(Collections.nCopies(minSize + 1, null));

        int i;
        for (i = 0; i < minSize; i++)
        {
            String primaryKey   = getUserEnteredValue(primaryKeyData, i, 0);
            String secondaryKey = getUserEnteredValue(secondaryKeyData, i, 0);
            String time         = getUserEnteredValue(timeData, i, 0);

            if (primaryKey != null && secondaryKey != null && time != null) {
                rows.set(i, new FilkArchiveEntryIndex(primaryKey, secondaryKey, time, secondaryKeyComparator));
            }
        }

        for (i = 0; i < rows.size(); i++)
        {
            if (rows.get(i) == null)
            {
                if (i + 1 < rows.size() && rows.get(i + 1) != null)
                {
                    FilkArchiveEntryIndex nextRow = rows.get(i + 1);
                    rows.set(i, new FilkArchiveEntryIndex(nextRow.primaryKey, null, null, secondaryKeyComparator));
                }
            }
        }

        while (rows.size() > 0 && rows.get(rows.size() - 1) == null)
        {
            rows.remove(rows.size() - 1);
        }

        return rows;
    }

    private void insertSeparator(ArrayList<Request> requests, int sheetId, int loc)
    {
        InsertDimensionRequest insertDimension = new InsertDimensionRequest().setInheritFromBefore(loc > 0).
            setRange(new DimensionRange().setSheetId(sheetId).setDimension("ROWS").setStartIndex(loc).setEndIndex(loc+1));
        requests.add(new Request().setInsertDimension(insertDimension));
    }

    private void insertRow(ArrayList<Request> requests, int sheetId, int loc, FilkArchiveEntry entry,
        Map<String, Integer> columnMap)
    {
        InsertDimensionRequest insertDimension = new InsertDimensionRequest().setInheritFromBefore(loc > 0).
            setRange(new DimensionRange().setSheetId(sheetId).setDimension("ROWS").setStartIndex(loc).setEndIndex(loc+1));
        requests.add(new Request().setInsertDimension(insertDimension));

        CellFormat cellFormat = new CellFormat().setTextFormat(new TextFormat().setForegroundColorStyle(new ColorStyle().setThemeColor("ACCENT2")));

        ArrayList<CellData> row = new ArrayList<>();

        for (Map.Entry<String, Integer> column: columnMap.entrySet())
        {
            int columnId = column.getValue();

            String data = entry.getColumnValue(column.getKey());
            if (data == null)
            {
                continue;
            }

            CellData cell = new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(data));

            cell.setUserEnteredFormat(cellFormat);

            if (columnId >= row.size())
            {
                row.addAll(Collections.nCopies(columnId - row.size() + 1, null));
            }

            row.set(columnId, cell);
        }

        UpdateCellsRequest update = new UpdateCellsRequest().setRows(Collections.singletonList(
            new RowData().setValues(row))).
            setFields("*").
            setStart(new GridCoordinate().setColumnIndex(0).setRowIndex(loc).setSheetId(sheetId));
        requests.add(new Request().setUpdateCells(update));
    }

    public void addNewRows(int sheetId,
        NavigableMap<FilkArchiveEntryIndex, FilkArchiveEntry> entries,
        Map<String, Integer> columnMap,
        String primaryKeyColumnId,
        String secondaryKeyColumnId,
        Comparator<String> secondaryKeyComparator)
        throws IOException, InterruptedException
    {
        ArrayList<Request> requests = new ArrayList<>();
        int rowOffset = 1;

        List<FilkArchiveEntryIndex> existingRows =
            getRows(sheetId,
                columnMap.get(primaryKeyColumnId),
                columnMap.get(secondaryKeyColumnId),
                columnMap.get("time"),
                secondaryKeyComparator);

        FilkArchiveEntryIndex lastIndex = null;

        for (Map.Entry<FilkArchiveEntryIndex, FilkArchiveEntry> entry: entries.entrySet())
        {
            FilkArchiveEntryIndex key = entry.getKey();
            int index = Collections.binarySearch(existingRows, key);
            if (index >= 0)
            {
                /* Entry is already in sheet */
                lastIndex = null;
                continue;
            }

            int insertionPoint = -index - 1;

            if (!((lastIndex != null && lastIndex.primaryKey.equals(key.primaryKey)) ||
                (insertionPoint > 0 && insertionPoint - 1 < existingRows.size() && existingRows.get(insertionPoint - 1).primaryKey.equals(key.primaryKey))))
            {
                insertSeparator(requests, sheetId, insertionPoint + rowOffset);
                rowOffset++;
            }

            insertRow(requests, sheetId,insertionPoint + rowOffset, entry.getValue(), columnMap);
            rowOffset++;
            lastIndex = key;
        }

        if (requests.isEmpty())
        {
            return;
        }

        BatchUpdateSpreadsheetRequest body
            = new BatchUpdateSpreadsheetRequest().setRequests(requests);

        BatchUpdateSpreadsheetResponse batchResponse =
            ExponentialBackoff.execute( () ->
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, body).execute()
            );

        batchResponse.size();
    }
}
