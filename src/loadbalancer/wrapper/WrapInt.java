package loadbalancer.wrapper;

public class WrapInt {
    private int number;

    public WrapInt() {
        number = 0;
    }

    public synchronized void add(int addends) {
        number += addends;
    }

    public synchronized int additiveModulo() {
        number = (number + 1) % 2000000000;
        return number;
    }

    public int getInt() {
        return number;
    }
}
