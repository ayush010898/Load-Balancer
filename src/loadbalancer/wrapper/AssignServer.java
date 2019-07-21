package loadbalancer.wrapper;

public class AssignServer {
    private int number;
    private WrapInt serverCount;

    public AssignServer(WrapInt sC) {
        number = 0;
        serverCount = sC;
    }

    public synchronized int getServerID() {
        int count = serverCount.getInt();
        if (count == 0) {
            return -1;
        }
        number = (number + 1) % count;
        return number;
    }
}

