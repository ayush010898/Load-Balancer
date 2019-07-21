package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Server implements Runnable {
    private DataInputStream dis;
    private DataOutputStream dos;
    private final ThreadGroup computeGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "compute");

    private Server() {
        try {
            Socket socket = new Socket("localhost", 1024);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF(Integer.toString(Runtime.getRuntime().availableProcessors()));
            dos.flush();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(10);
        computeGroup.setMaxPriority(5);
        try {
            while (true) {
                String key = dis.readUTF();
                int id = Integer.parseInt(dis.readUTF());
                System.out.println(key);
                Computation computation = new Computation(dos, key, id);
                Thread compute = new Thread(computeGroup, computation);
                compute.start();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        new Thread(server).start();
        while (true) {
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                System.out.println("Main Thread Sleeping.");
            }
        }
    }
}