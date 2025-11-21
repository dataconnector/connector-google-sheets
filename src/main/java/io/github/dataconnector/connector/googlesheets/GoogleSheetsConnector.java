package io.github.dataconnector.connector.googlesheets;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;

import io.github.dataconnector.spi.DataSink;
import io.github.dataconnector.spi.DataSource;
import io.github.dataconnector.spi.DataStreamSink;
import io.github.dataconnector.spi.DataStreamSource;
import io.github.dataconnector.spi.model.ConnectorContext;
import io.github.dataconnector.spi.model.ConnectorMetadata;
import io.github.dataconnector.spi.model.ConnectorResult;
import io.github.dataconnector.spi.stream.StreamCancellable;
import io.github.dataconnector.spi.stream.StreamObserver;
import io.github.dataconnector.spi.stream.StreamWriter;

public class GoogleSheetsConnector implements DataSource, DataSink, DataStreamSource, DataStreamSink {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsConnector.class);
    private static final String APPLICATION_NAME = "Universal Data Connector";
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public String getType() {
        return "google-sheets";
    }

    @Override
    public ConnectorMetadata getMetadata() {
        return ConnectorMetadata.builder()
                .name("Google Sheets Connector")
                .description("Read/Write Google Sheets via API v4. Supports Service Account and OAuth2 authentication.")
                .version("0.0.1")
                .author("Hai Pham Ngoc <ngochai285nd@gmail.com>")
                .build();
    }

    @Override
    public List<String> validateConfiguration(ConnectorContext context) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> config = context.getConfiguration();

        if (config == null) {
            errors.add("Configuration is missing");
            return errors;
        }

        if (!config.containsKey("spreadsheet_id")) {
            errors.add("Missing required configuration: spreadsheet_id");
        }

        boolean hasServiceAccount = config.containsKey("credential_path") || config.containsKey("credential_json");
        boolean hasAccessToken = config.containsKey("access_token");

        if (!hasServiceAccount && !hasAccessToken) {
            errors.add("Either service account credentials or access token must be provided");
        }

        return errors;
    }

    @Override
    public StreamWriter createWriter(ConnectorContext context) throws IOException {
        try {
            return new GoogleSheetsStreamWriter(context);
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to create Google Sheets service", e);
        }
    }

    @Override
    public StreamCancellable startStream(ConnectorContext context, StreamObserver observer) throws Exception {
        AtomicBoolean running = new AtomicBoolean(true);

        String spreadsheetId = context.getConfiguration("spreadsheet_id", String.class).orElseThrow();
        String sheetName = context.getConfiguration("sheet_name", String.class).orElse("Sheet1");
        boolean headerRow = context.getConfiguration("header_row", Boolean.class).orElse(true);
        int batchSize = context.getConfiguration("batch_size", Integer.class).orElse(500);
        int limit = context.getConfiguration("limit", Integer.class).orElse(-1);
        Set<Integer> includedColumns = parseIncludedColumns(context);

        executorService.submit(() -> {
            try {
                Sheets service = createSheetsService(context);
                logger.info("Started streaming from Google Sheets: {} - {}", spreadsheetId, sheetName);

                List<String> headers = new ArrayList<>();
                int startRowIndex = 1;

                if (headerRow) {
                    String headerRange = String.format("%s!A1:ZZ1", sheetName);
                    ValueRange headerResponse = service.spreadsheets().values().get(spreadsheetId, headerRange)
                            .execute();
                    if (headerResponse.getValues() != null && !headerResponse.getValues().isEmpty()) {
                        for (Object cell : headerResponse.getValues().get(0)) {
                            headers.add(cell != null ? cell.toString() : "");

                        }
                    }
                    startRowIndex = 2;
                }

                int currentOffset = startRowIndex;
                int totalProcessed = 0;
                boolean hasMoreData = true;

                while (running.get() && hasMoreData) {
                    if (limit != -1 && totalProcessed >= limit) {
                        break;
                    }

                    int endRowIndex = currentOffset + batchSize - 1;
                    String range = String.format("%s!A%d:ZZ%d", sheetName, currentOffset, endRowIndex);

                    logger.debug("Fetching rows from {} to {}", currentOffset, endRowIndex);
                    ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
                    List<List<Object>> values = response.getValues();

                    if (values == null || values.isEmpty()) {
                        hasMoreData = false;
                    } else {
                        for (List<Object> row : values) {
                            if (limit != -1 && totalProcessed >= limit) {
                                break;
                            }

                            if (row.isEmpty()) {
                                continue;
                            }

                            Map<String, Object> record = mapRowToRecord(row, headers, includedColumns);
                            observer.onNext(record);
                            totalProcessed++;
                        }
                        currentOffset += values.size();
                        if (values.size() < batchSize) {
                            hasMoreData = false;
                        }
                    }
                }

                logger.info("Finished streaming from Google Sheets: {} - {} ({} rows)", spreadsheetId, sheetName,
                        totalProcessed);
                observer.onComplete();
            } catch (Exception e) {
                logger.error("Error streaming from Google Sheets: {} - {}", spreadsheetId, sheetName, e);
                observer.onError(e);
            }
        });

        return () -> {
            logger.info("Stopping streaming from Google Sheets: {} - {}", spreadsheetId, sheetName);
            running.set(false);
        };
    }

    @Override
    public ConnectorResult write(ConnectorContext context, List<Map<String, Object>> records) throws Exception {
        long startTime = System.currentTimeMillis();

        if (records == null || records.isEmpty()) {
            return ConnectorResult.builder()
                    .success(true)
                    .message("No records to write")
                    .recordsProcessed(0)
                    .records(records)
                    .executionTimeMillis(System.currentTimeMillis() - startTime)
                    .build();
        }

        try (StreamWriter writer = createWriter(context)) {
            writer.writeBatch(records);
        }

        return ConnectorResult.builder()
                .success(true)
                .message("Successfully wrote " + records.size() + " rows to Google Sheets")
                .recordsProcessed(records.size())
                .records(records)
                .executionTimeMillis(System.currentTimeMillis() - startTime)
                .build();
    }

    @Override
    public ConnectorResult read(ConnectorContext context) throws Exception {
        long startTime = System.currentTimeMillis();

        Sheets service = createSheetsService(context);
        String spreadsheetId = context.getConfiguration("spreadsheet_id", String.class).orElseThrow();
        String range = context.getConfiguration("range", String.class).orElse("Sheet1");
        boolean headerRow = context.getConfiguration("header_row", Boolean.class).orElse(true);
        Set<Integer> includedColumns = parseIncludedColumns(context);
        if (includedColumns != null) {
            logger.debug("Restricting read() columns: {}", includedColumns);
        }

        ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();
        List<Map<String, Object>> records = new ArrayList<>();

        if (values != null && !values.isEmpty()) {
            records = parseRows(values, headerRow, includedColumns);
        }

        return ConnectorResult.builder()
                .success(true)
                .message("Successfully fetched " + records.size() + " rows from Google Sheets")
                .recordsProcessed(records.size())
                .records(records)
                .executionTimeMillis(System.currentTimeMillis() - startTime)
                .build();

    }

    private Sheets createSheetsService(ConnectorContext context) throws IOException, GeneralSecurityException {
        String accessToken = context.getConfiguration("access_token", String.class).orElse(null);

        if (accessToken != null) {
            GoogleCredentials credentials;
            String refreshToken = context.getConfiguration("refresh_token", String.class).orElse(null);
            String clientId = context.getConfiguration("client_id", String.class).orElse(null);
            String clientSecret = context.getConfiguration("client_secret", String.class).orElse(null);

            if (refreshToken != null && clientId != null && clientSecret != null) {
                logger.info("Initializing Google Sheets Service with OAuth2 Refresh Token");
                credentials = UserCredentials.newBuilder()
                        .setClientId(clientId)
                        .setClientSecret(clientSecret)
                        .setRefreshToken(refreshToken)
                        .setAccessToken(new AccessToken(accessToken, null))
                        .build();
            } else {
                logger.info("Initializing Google Sheets Service with Access Token");
                credentials = GoogleCredentials.create(new AccessToken(accessToken, null));
            }

            return new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }

        String credentialPath = context.getConfiguration("credential_path", String.class).orElse(null);
        Object credentialJson = context.getConfiguration().get("credential_json");

        InputStream credentialsStream;
        if (credentialJson instanceof String) {
            logger.info("Initializing Google Sheets Service with Service Account (JSON Content)");
            credentialsStream = new ByteArrayInputStream(((String) credentialJson).getBytes(StandardCharsets.UTF_8));
        } else if (credentialPath != null) {
            File file = new File(credentialPath);
            if (file.exists()) {
                credentialsStream = new FileInputStream(file);
            }

            URL url = getClass().getClassLoader().getResource(credentialPath);
            if (url != null) {
                logger.info("Initializing Google Sheets Service with Service Account (URL: {})", url);
                credentialsStream = url.openStream();
            }
            throw new IOException("Service account file not found: " + credentialPath);
        } else {
            throw new IllegalArgumentException("Either credential_json or credential_path must be provided");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private List<Map<String, Object>> parseRows(List<List<Object>> values, boolean headerRow,
            Set<Integer> includedColumns) {
        List<Map<String, Object>> records = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            return records;
        }

        List<String> headers = new ArrayList<>();
        int startIndex = 0;
        if (headerRow) {
            List<Object> firstRow = values.get(0);
            for (Object cell : firstRow) {
                headers.add(cell != null ? cell.toString() : "");
            }
            startIndex = 1;
        } else {
            int numColumns = values.get(0).size();
            for (int i = 0; i < numColumns; i++) {
                headers.add("Column_" + (i + 1));
            }
        }

        for (int i = startIndex; i < values.size(); i++) {
            records.add(mapRowToRecord(values.get(i), headers, includedColumns));
        }
        return records;
    }

    private Map<String, Object> mapRowToRecord(List<Object> row, List<String> headers, Set<Integer> includedColumns) {
        Map<String, Object> record = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            if (includedColumns != null && !includedColumns.contains(i)) {
                continue;
            }

            String key = headers.get(i);
            Object value = (i < row.size()) ? row.get(i) : null;
            record.put(key, value);
        }
        return record;
    }

    private Set<Integer> parseIncludedColumns(ConnectorContext context) {
        Object columnsConfig = context.getConfiguration().get("columns");
        if (columnsConfig == null) {
            return null;
        }

        Set<Integer> indices = new HashSet<>();
        try {
            if (columnsConfig instanceof String) {
                String[] parts = ((String) columnsConfig).split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.contains("-")) {
                        String[] range = part.split("-");
                        if (range.length != 2) {
                            throw new IllegalArgumentException("Invalid column range: " + part);
                        }
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        for (int i = start; i <= end; i++) {
                            indices.add(i);
                        }
                    } else {
                        indices.add(Integer.parseInt(part));
                    }
                }
            } else if (columnsConfig instanceof List) {
                for (Object item : (List<?>) columnsConfig) {
                    indices.add(Integer.parseInt(item.toString()));
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse 'columns' configuration. Reading all columns.", e);
            return null;
        }
        return indices.isEmpty() ? null : indices;
    }

    private class GoogleSheetsStreamWriter implements StreamWriter {
        private final Sheets service;
        private final String spreadsheetId;
        private final String range;
        private final int bufferSize;
        private final List<List<Object>> buffer;
        private boolean isClosed = false;
        private List<String> headers = null;
        private final ConnectorContext context;

        public GoogleSheetsStreamWriter(ConnectorContext context) throws IOException, GeneralSecurityException {
            this.service = createSheetsService(context);
            this.spreadsheetId = context.getConfiguration("spreadsheet_id", String.class).orElseThrow();
            this.range = context.getConfiguration("range", String.class).orElse("Sheet1");
            this.bufferSize = context.getConfiguration("batch_size", Integer.class).orElse(500);
            this.buffer = new ArrayList<>();
            this.context = context;
        }

        @Override
        public void close() throws IOException {
            if (isClosed) {
                return;
            }
            isClosed = true;
            flushBuffer();
        }

        @Override
        public void writeBatch(List<Map<String, Object>> records) throws IOException {
            if (isClosed) {
                throw new IOException("Stream writer is closed");
            }

            if (records == null || records.isEmpty()) {
                return;
            }

            if (headers == null) {
                headers = new ArrayList<>(records.get(0).keySet());
            }

            Set<Integer> includedColumns = parseIncludedColumns(context);

            for (Map<String, Object> record : records) {
                List<Object> row = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    if (includedColumns != null && !includedColumns.contains(i)) {
                        continue;
                    }
                    row.add(record.getOrDefault(headers.get(i), ""));
                }
                buffer.add(row);

                if (buffer.size() >= bufferSize) {
                    flushBuffer();
                }
            }
        }

        private void flushBuffer() throws IOException {
            if (buffer.isEmpty()) {
                return;
            }

            ValueRange body = new ValueRange().setValues(buffer);
            service.spreadsheets().values()
                    .append(spreadsheetId, range, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
            logger.debug("Flushed {} rows to Google Sheets", buffer.size());
            buffer.clear();
        }
    }

}
