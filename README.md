# Google Sheets Connector

Google Sheets Connector is a Universal Data Connector implementation that enables bi-directional data exchange with Google Sheets via the Sheets API v4. It implements the `DataSource`, `DataSink`, `DataStreamSource`, and `DataStreamSink` contracts so it can run both batch and streaming jobs inside the `io.github.dataconnector` ecosystem.

## Features

- Read tabular data from any spreadsheet range with optional header detection
- Write batches of records with configurable buffer sizes
- Continuous streaming support with graceful cancellation
- Service Account (JSON file or inline) and OAuth2 (access/refresh token) authentication
- Configurable batching, sheet/range selection, and row limits
- Structured configuration validation and detailed telemetry via SLF4J

## Requirements

- Java 17 or later
- Maven 3.9+
- Google Cloud project with the **Google Sheets API** enabled
- Credentials with at least read access to the target spreadsheet

## Installation

Add the connector dependency to your Maven project:

```xml
<dependency>
    <groupId>io.github.dataconnector</groupId>
    <artifactId>connector-google-sheets</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Configuration

Provide the following configuration keys through `ConnectorContext`:

| Key | Type | Required | Description |
| --- | --- | --- | --- |
| `spreadsheet_id` | string | yes | The spreadsheet ID visible in the sheet URL |
| `sheet_name` | string | no | Sheet/tab name. Default: `Sheet1` |
| `range` | string | no | Range like `Sheet1!A1:Z100` used by `read`/writer |
| `header_row` | boolean | no | Treat first row as headers. Default: `true` |
| `batch_size` | integer | no | Rows fetched/appended per call. Default: `500` |
| `limit` | integer | no | Max rows streamed (`-1` = unlimited) |
| `credential_path` | string | *xor* | Path or classpath resource to a Service Account JSON |
| `credential_json` | string | *xor* | Raw JSON string for Service Account credentials |
| `access_token` | string | *xor* | OAuth2 access token |
| `refresh_token` | string | optional | OAuth2 refresh token (requires `client_id`/`client_secret`) |
| `client_id` / `client_secret` | string | optional | OAuth2 client credentials used with refresh tokens |

Exactly one authentication strategy must be configured:

1. **Service Account**: provide `credential_path` or `credential_json`
2. **OAuth2 Access Token**: provide `access_token` (optionally with refresh token)

## Usage

### Read once

```java
ConnectorContext context = ConnectorContext.builder()
    .configuration(Map.of(
        "spreadsheet_id", "1AbCxyz...",
        "range", "Orders!A1:F200",
        "header_row", true))
    .build();

GoogleSheetsConnector connector = new GoogleSheetsConnector();
ConnectorResult result = connector.read(context);
List<Map<String, Object>> rows = result.getRecords();
```

### Stream rows

```java
StreamObserver observer = new StreamObserver() {
    public void onNext(Map<String, Object> item) { /* handle row */ }
    public void onComplete() { /* done */ }
    public void onError(Throwable error) { /* handle */ }
};

StreamCancellable cancellable = connector.startStream(context, observer);
// ... later
cancellable.cancel();
```

### Write data

```java
List<Map<String, Object>> payload = List.of(
    Map.of("Name", "Alice", "Email", "alice@example.com"),
    Map.of("Name", "Bob", "Email", "bob@example.com")
);

ConnectorResult writeResult = connector.write(context, payload);
```

## Development

```bash
mvn clean verify
```

- Ensure `CHANGELOG.md` is updated for user-facing changes
- Follow [CONTRIBUTING.md](./CONTRIBUTING.md) for commit and PR guidelines

## Versioning

This project follows [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html) and documents releases in [CHANGELOG.md](./CHANGELOG.md). Tags are published as `vMAJOR.MINOR.PATCH`.

## License

Distributed under the [Apache License 2.0](./LICENSE).
