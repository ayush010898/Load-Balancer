package loadbalancer;

import loadbalancer.queue.ClientRequest;
import loadbalancer.queue.DynamicQueue;
import loadbalancer.queue.ThreadQueue;
import loadbalancer.wrapper.WrapBoolean;
import loadbalancer.wrapper.WrapInt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ServerProcess implements Runnable {
    // Server Socket
    private final Socket socket;

    // Request/Response Queues
    private final ArrayList<DynamicQueue> requestQueues;
    private final ArrayList<DynamicQueue> responseQueues;

    // Interrupt Thread Queue
    private final ArrayList<ThreadQueue> threadQueues;

    // Request/Response Queue Of This Server
    private DynamicQueue requestQueue;
    private DynamicQueue responseQueue;
    private final DynamicQueue tempRequestQueue = new DynamicQueue();

    // Thread Queue Of This Server
    private ThreadQueue threadQueue;

    // Wrapper Classes
    private final WrapInt count = new WrapInt();
    private final WrapInt serverCount;
    private final WrapBoolean serverAlive = new WrapBoolean();

    private final int serverID;

    ServerProcess(Socket socket, ArrayList<DynamicQueue> requestQueues, ArrayList<DynamicQueue> responseQueues, ArrayList<ThreadQueue> threadQueues, WrapInt serverCount, int serverID) {
        this.socket = socket;
        this.requestQueues = requestQueues;
        this.responseQueues = responseQueues;
        this.threadQueues = threadQueues;
        this.serverID = serverID;
        this.serverCount = serverCount;

        requestQueue = requestQueues.get(serverID);
        responseQueue = responseQueues.get(serverID);
        threadQueue = this.threadQueues.get(serverID);
    }

    @Override
    public void run() {
        try {
            final DataInputStream dis = new DataInputStream(socket.getInputStream());
            final DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            ClientRequest tempClientRequest;

            // First Message Of Server Is The Number Of Logical Cores
            int maxRequests = Integer.parseInt(dis.readUTF());

            // Starting Response Listener
            ResponseListener responseListener = new ResponseListener(dis, responseQueue, tempRequestQueue, threadQueue, count, serverAlive);
            new Thread(responseListener).start();

            // Listener For Sending Request To Assigned Server
            while (true) {
                if (!serverAlive.getStatus()) {
                    throw new IOException();
                }
                if (requestQueue.size() > 0 && count.getInt() < maxRequests) {
                    tempClientRequest = requestQueue.dequeueClientRequest();
                    tempRequestQueue.enqueue(tempClientRequest.request, tempClientRequest.id);
                    dos.writeUTF(tempClientRequest.request);
                    dos.writeUTF(Integer.toString(tempClientRequest.id));
                    dos.flush();
                    count.add(1);
                }
            }
        } catch (IOException e) {
            System.out.println("Server Disconnected." + serverID);
            responseQueues.remove(responseQueue);
            ClientRequest temp;

            // Arrange Server Requests & Remove Server
            while (true) {
                temp = requestQueue.dequeueClientRequest();
                if (temp.id == -1) {
                    requestQueues.remove(requestQueue);
                    threadQueues.remove(threadQueue);
                    serverCount.add(-1);
                    break;
                }
                tempRequestQueue.enqueue(temp.request, temp.id);
            }

            // Re-Direct Requests
            int redirectServer = 0;
            int remainingServers;
            while (true) {
                remainingServers = responseQueues.size() - 1;
                if (remainingServers > 0) {
                    if (tempRequestQueue.size() > 0) {
                        temp = tempRequestQueue.dequeueClientRequest();
                        requestQueues.get(redirectServer).enqueue(temp.request, temp.id);
                        threadQueues.get(redirectServer).enqueue(threadQueue.dequeue());
                        redirectServer = (redirectServer + 1) % remainingServers;
                    }
                    else {
                        break;
                    }
                }
            }
            System.out.println("Re-Directed Requests. Server Removed.");
        }
    }
}