# CS471: Internet File Downloader

## Class Diagram
---
```mermaid
classDiagram
class MainManager{
-ThreadpoolExecutor fileManger;
-DatabaseConnection dbConn;
-ConcurrentHashMap<> inProgress;
-ConcurrentLinkedQueue<> queuedDownloads;

+add(String uri) void;
+addAll(ArrayList<String> uris) void;
+pause(String id) Boolean;
+resume(String id) Boolean;
+pauseAll() void;
+resumeAll() void;

-initDownload(String uri, String dest) Boolean;
-createDbEntry(FileDownload down) void;
}
MainManager <-- FileDownloader
Callable~Boolean~ <|-- FileDownloader
class FileDownloader{
-String uri
-String dest
-Integer size 
-FtpClient serverConn 
-InputStream fileReader
-OutputStream fileWriter
-Integer retryAttempts
-NotSureYet segmentManager


-startDownload() void
-pauseDownload() void
-retrySegement() void
}

FileDownloader <-- SegmentDownloader
Callable~Boolean~ <|-- SegmentDownloader
class SegmentDownloader{
-FtpClient serverConn 
-InputStream fileReader
-OutputStream fileWriter
-String uri
-String dest

+SegmentDownloader
-call() Boolean;
-suspendSegement() void
}
```
---
## Class Descriptions
---
- **MainManger**: responsible for linking between GUI and logic, opening the DB conn, and loading user preferences.
- **FileDownloader**: responsible for allocating space and file,  managing the threads that download the file in chunks, recombining the chunks, and if paused storing the status of the file in DB for later continuation.
- **SegmentDownloader**: responsible for creating passive FTP link with server and downloading the chunk from a provided offset into a *.part* file.
