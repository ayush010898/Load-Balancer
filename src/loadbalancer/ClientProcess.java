package loadbalancer;

import loadbalancer.queue.DynamicQueue;
import loadbalancer.queue.ThreadQueue;
import loadbalancer.wrapper.AssignServer;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

public class ClientProcess implements Runnable {
    // File Root Directory
    private final File WEB_ROOT = new File(".");

    // Verbose Mode
    private final boolean verbose;

    // Client Socket
    private final Socket connect;

    // For Request Input stream
    private BufferedReader in = null;
    // For Header Output Stream
    private PrintWriter out = null;
    // For Response Output Stream
    private BufferedOutputStream dataOut = null;

    // Request/Response Queues Of Server Assigned
    private final ArrayList<DynamicQueue> requestQueues;
    private final ArrayList<DynamicQueue> responseQueues;

    // Interrupt Thread Queue
    private final ArrayList<ThreadQueue> threadQueues;

    // Wrapper Classes
    private final AssignServer assignServer;

    // Assigned ID Of Client Thread
    private final int clientID;

    ClientProcess(Socket connect, ArrayList<DynamicQueue> requestQueues, ArrayList<DynamicQueue> responseQueues, ArrayList<ThreadQueue> threadQueues, AssignServer assignServer, int clientID, boolean verbose) {
        this.connect = connect;
        this.requestQueues = requestQueues;
        this.responseQueues = responseQueues;
        this.threadQueues = threadQueues;
        this.assignServer = assignServer;
        this.verbose = verbose;
        this.clientID = clientID;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            out = new PrintWriter(connect.getOutputStream());
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            String input = in.readLine();
            if (input == null) return;

            // Parse The Request With A String Tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase();
            String path = parse.nextToken();

            if (verbose) {
                System.out.println("URL: " + input);
                // System.out.println("Method: " + method);
                // System.out.println("Path: " + path);
            }

            if (!method.equals("GET") && !method.equals("HEAD")) {
                // Return The Not Supported File To The Client
                if (verbose) {
                    System.out.println("501 Not Implemented: " + method + " method");
                }

                byte[] fileData = readFileData("not_supported.html");
                sendHtmlResponse(fileData, "501 Not Implemented");

            } else {
                // Support Only GET And HEAD Methods

                if (path.equals("/")) {
                    // We Return The Index Page
                    byte[] fileData = readFileData("index.html");
                    sendHtmlResponse(fileData, "200 OK");

                } else if (path.regionMatches(true, 0, "/?key=", 0, 6)) {
                    // Return Decrypted Key To Client

                    // Wait Until A Server Becomes Available To Redirect Server
                    int currentServer = -1;
                    while (currentServer == -1) {
                        currentServer = assignServer.getServerID();
                    }
                    threadQueues.get(currentServer).enqueue(Thread.currentThread());
                    DynamicQueue requestQueue = requestQueues.get(currentServer);
                    DynamicQueue responseQueue = responseQueues.get(currentServer);
                    requestQueue.enqueue(path, clientID);

                    // Sleep Until Server Finishes Processing Request
                    try {
                        Thread.sleep(Integer.MAX_VALUE);
                    } catch (InterruptedException e) {
                        String output;

                        // Check If Response Client ID Matches, Else Poll The Servers For Response (Server Crash Cases)
                        while (true) {
                            if (responseQueue.getClientID() == clientID){
                                output = responseQueue.dequeue();
                                break;
                            }
                            else {
                                currentServer = assignServer.getServerID();
                                responseQueue = responseQueues.get(currentServer);
                            }
                        }

                        // Return The Response
                        String response = "<!DOCTYPE html><head><title>Requested Data</title></head><body><p>" + output + "</p></body></html>";
                        byte[] responseData = response.getBytes();
                        sendHtmlResponse(responseData, "200 OK");
                    }

                } else {
                    // Return 404 Page
                    String response = "<!DOCTYPE html><head><title>404 Not Found</title></head><body><h1>Not Found</h1><p>The requested URL " + path + " was not found on this loadbalancer.</p></body></html>";
                    byte[] responseData = response.getBytes();
                    sendHtmlResponse(responseData, "404 Not Found");
                }
            }
        } catch (IOException e) {
            System.out.println("Server Error: " + e);
        } finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                connect.close();
            } catch (IOException e) {
                System.out.println("Server Error: Client Disconnected - Broken Pipeline");
            }
        }
    }

    private void sendHtmlResponse(byte[] fileData, String responseCode) throws IOException {
        // Header
        out.println("HTTP/1.1 " + responseCode);
        out.println(("Server: Java HTTP Server: 1.0"));
        out.println("Date: " + new Date());
        out.println("Content-type: text/html");
        out.println("Content-length: " + fileData.length);

        // Blank line between headers and content
        out.println();
        out.flush();

        // Response file
        dataOut.write(fileData, 0, fileData.length);
        dataOut.flush();
    }

    private byte[] readFileData(String fileName) throws IOException {
        File file = new File(WEB_ROOT, fileName);
        int fileLength = (int) file.length();
        byte[] fileData = new byte[fileLength];

        // Read content to return to client
        try(FileInputStream fileIn = new FileInputStream(file)) {
            fileIn.read(fileData);
            return fileData;
        }
    }
}