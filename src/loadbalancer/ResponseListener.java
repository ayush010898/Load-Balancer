package loadbalancer;

import loadbalancer.queue.DynamicQueue;
import loadbalancer.queue.ThreadQueue;
import loadbalancer.wrapper.WrapBoolean;
import loadbalancer.wrapper.WrapInt;

import java.io.DataInputStream;
import java.io.IOException;

public class ResponseListener implements Runnable {
    // Response Queue Of This Server
    private final DynamicQueue responseQueue;

    // Thread Queue Of This Server
    private final ThreadQueue threadQueue;

    // Temporary Request Queue From Server Process
    private final DynamicQueue tempRequestQueue;

    // Input Stream To Read Responses From Server
    private final DataInputStream dis;

    // Wrapper classes
    private final WrapInt count;
    private final WrapBoolean serverAlive;

    ResponseListener(DataInputStream dataInputStream, DynamicQueue responseQueue, DynamicQueue tempRequestQueue, ThreadQueue threadQueue, WrapInt count, WrapBoolean serverAlive) {
        this.responseQueue = responseQueue;
        this.tempRequestQueue = tempRequestQueue;
        this.dis = dataInputStream;
        this.threadQueue = threadQueue;
        this.count = count;
        this.serverAlive = serverAlive;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String str = dis.readUTF();
                int id = Integer.parseInt(dis.readUTF());
                responseQueue.enqueue(str, id);
                Thread client = threadQueue.dequeue();
                client.interrupt();
                count.add(-1);
                tempRequestQueue.dequeue();
            }
        } catch (IOException e) {
            serverAlive.killStatus();
        }
    }
}