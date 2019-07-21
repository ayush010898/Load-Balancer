package loadbalancer;


import loadbalancer.queue.DynamicQueue;
import loadbalancer.queue.ThreadQueue;
import loadbalancer.wrapper.AssignServer;
import loadbalancer.wrapper.WrapInt;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Date;

// Each Client Connection will be managed in a dedicated thread
public class HTTPServer {
    // Port to listen for connection
    private static final int PORT = 8080;
    // Port to listen for connection
    private static final int SERVER_PORT = 1024;

    // Verbose Mode
    private static final boolean verbose = true;

    // Thread Groups
    private static final ThreadGroup mainGroup = Thread.currentThread().getThreadGroup();
    private static final ThreadGroup clientGroup = new ThreadGroup(mainGroup, "client");
    private static final ThreadGroup serverListenerGroup = new ThreadGroup(mainGroup, "loadbalancer-listener");
    private static final ThreadGroup serverGroup = new ThreadGroup(serverListenerGroup, "loadbalancer");

    // Request/Response Queues
    private static ArrayList<DynamicQueue> requestQueues = new ArrayList<>();
    private static ArrayList<DynamicQueue> responseQueues = new ArrayList<>();

    // Interrupt Thread Queue
    private static ArrayList<ThreadQueue> threadQueues = new ArrayList<>();

    // Integer Wrapper Classes
    private static WrapInt serverCount = new WrapInt();
    private static WrapInt clientID = new WrapInt();
    private static AssignServer assignServer = new AssignServer(serverCount);

    public static void main(String[] args) {
        // Setting priority of thread groups. load-balancer same as http loadbalancer priority
        // Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        clientGroup.setMaxPriority(3);
        serverGroup.setMaxPriority(5);
        serverListenerGroup.setMaxPriority(1);

        // Starting loadbalancer listener
        ServerSocket serverConnect = null;
        ServerListener Listener = new ServerListener(SERVER_PORT, requestQueues, responseQueues, threadQueues, serverGroup, serverCount);
        new Thread(serverListenerGroup, Listener).start();

        // Locking loadbalancer port
        try {
            serverConnect = new ServerSocket(PORT);
            System.out.println("Server Started.\n Listening for connections on port: " + PORT + " ...\n");
        } catch (IOException e) {
            System.out.println("Server Start Error: " + e.getMessage());
        }

        // Starting Http client listener
        try {
            while (true) {
                if (clientGroup.activeCount() < 1) {
                    // Accept client connections and assign it to a dedicated thread
                    ClientProcess client = new ClientProcess(serverConnect.accept(), requestQueues, responseQueues, threadQueues, assignServer, clientID.additiveModulo(), verbose);
                    new Thread(clientGroup, client).start();

                    if (verbose)
                        System.out.println("Connection opened. (" + new Date() + ")");
                }
            }
        } catch (IOException | NullPointerException e) {
            System.out.println("Server Connection Error: " + e.getMessage());
        }
    }
}