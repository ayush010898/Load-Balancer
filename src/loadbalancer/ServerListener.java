package loadbalancer;

import loadbalancer.queue.DynamicQueue;
import loadbalancer.queue.ThreadQueue;
import loadbalancer.wrapper.WrapInt;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class ServerListener implements Runnable {
    // Server Socket PORT
    private int PORT;

    // Request/Response Queues
    private final ArrayList<DynamicQueue> requestQueues;
    private final ArrayList<DynamicQueue> responseQueues;

    // Interrupt Thread Queue
    private final ArrayList<ThreadQueue> threadQueues;

    // Server Thread Group
    private final ThreadGroup serverGroup;

    // Wrapper Classes
    private final WrapInt serverCount;

    ServerListener(int port, ArrayList<DynamicQueue> requestQueues, ArrayList<DynamicQueue> responseQueues, ArrayList<ThreadQueue> threadQueues, ThreadGroup serverGroup, WrapInt serverCount) {
        PORT = port;
        this.requestQueues = requestQueues;
        this.responseQueues = responseQueues;
        this.threadQueues = threadQueues;
        this.serverGroup = serverGroup;
        this.serverCount = serverCount;
    }

    @Override
    public void run() {
        int serverID;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            System.out.println("Server Lister Failed to Initialize - Port Busy.");
        }
        if (serverSocket != null) {
            try {
                while (true) {
                    requestQueues.add(new DynamicQueue());
                    responseQueues.add(new DynamicQueue());
                    threadQueues.add(new ThreadQueue());
                    serverID = serverCount.getInt();
                    ServerProcess serverProcess = new ServerProcess(serverSocket.accept(), requestQueues, responseQueues, threadQueues, serverCount, serverID);
                    serverCount.add(1);
                    System.out.println("Server " + serverID + " Connected");
                    new Thread(serverGroup, serverProcess).start();
                }
            } catch (IOException e) {
                System.out.println("Server Lister Failed to Initialize - Port Busy.");
            }
        }
    }
}