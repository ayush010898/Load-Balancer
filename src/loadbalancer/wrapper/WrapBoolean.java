package loadbalancer.wrapper;

public class WrapBoolean {
    private boolean serverAlive = true;

    public boolean getStatus() {
        return serverAlive;
    }

    public void killStatus() {
        serverAlive = false;
    }
}
