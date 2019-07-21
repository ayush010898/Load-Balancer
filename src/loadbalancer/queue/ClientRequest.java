package loadbalancer.queue;

public class ClientRequest {
    public String request;
    public int id;

    ClientRequest(String r, int i) {
        request = r;
        id = i;
    }
}
