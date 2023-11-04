import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionManager {

    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    private ReentrantLock sendLock;
    private ReentrantLock messagesLock;
    private ReentrantLock readLock;
    private HashMap<String, Queue<String>> messagesQueue;
    private HashMap<String, Condition> conditions;

    private Queue<String> getQueue(String type) {
        return messagesQueue.computeIfAbsent(type, k -> new LinkedList<>());
    }

    ConnectionManager(Socket socket) throws IOException {
        this.socket = socket;
        this.in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out    = new PrintWriter(socket.getOutputStream(), true);
        this.sendLock = new ReentrantLock();
        this.readLock = new ReentrantLock();
        this.messagesLock = new ReentrantLock();
        this.messagesQueue = new HashMap<>();
        this.conditions = new HashMap<>();
        defineConditions();
    }

    public static ConnectionManager start(Socket socket) throws IOException {
        return new ConnectionManager(socket);
    };

    private void defineConditions() {
        for (int i = 1; i <= 10; i++) {
            String type = "top" + i;
            conditions.put(type, messagesLock.newCondition());
        }

        conditions.put("vizinhos", messagesLock.newCondition());
        conditions.put("stream", messagesLock.newCondition());
    }

    public void send(String type, String message) throws IOException {
        sendLock.lock();
        try {
            out.println(type + ":" + message);
        }
        finally {
            sendLock.unlock();
        }
    }

    public String receive(String type) throws InterruptedException {
        String msg;
        messagesLock.lock();
        try {
            Queue<String> queue = getQueue(type);
            msg = queue.poll();
            Condition condition = conditions.get(type);

            while (msg == null) {
                condition.await();
                msg = queue.poll();
            }

            return msg;
        }
        finally {
            messagesLock.unlock();
        }
    }

    public void read() throws IOException {
        String line;
        readLock.lock();
        try {
            line = in.readLine();

            if (line != null) {
                String[] aux = line.split(":");
                String type = aux[0];
                String msg = aux[1];

                messagesLock.lock();
                try {
                    Queue<String> queue = getQueue(type);
                    queue.offer(msg);
                    Condition condition = conditions.get(type);
                    condition.signalAll();
                } finally {
                    messagesLock.unlock();
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public boolean isOpen() {
        return socket.isConnected();
    }

    public void close() throws IOException {
        this.socket.close();
        this.in.close();
        this.out.close();
    }
}
