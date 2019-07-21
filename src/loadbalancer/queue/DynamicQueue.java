package loadbalancer.queue;

import java.util.ArrayList;

public class DynamicQueue {
    private ArrayList<ClientRequest> queue = new ArrayList<>();
    private int size = 0;

    public synchronized void enqueue(String temp, Integer id) {
        queue.add(new ClientRequest(temp, id));
        size += 1;
    }

    public synchronized String dequeue() {
        if (size == 0) {
            return "";
        }
        ClientRequest temp = queue.remove(0);
        size -= 1;
        return temp.request;
    }

    public synchronized ClientRequest dequeueClientRequest() {
        if (size == 0) {
            return new ClientRequest("", -1);
        }
        size -= 1;
        return queue.remove(0);
    }

    public synchronized int getClientID() {
        if (size == 0){
            return -1;
        }
        return queue.get(0).id;
    }

    public synchronized int size() {
        return size;
    }
}
