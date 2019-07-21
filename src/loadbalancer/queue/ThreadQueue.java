package loadbalancer.queue;

import java.util.ArrayList;

public class ThreadQueue {
    private ArrayList<Thread> threads = new ArrayList<>();
    private int count = 0;

    public synchronized void enqueue(Thread thread) {
        threads.add(thread);
        count += 1;
    }

    public synchronized Thread dequeue() {
        if (count == 0) {
            return null;
        }
        Thread thread = threads.remove(0);
        count -= 1;
        return thread;
    }
}
