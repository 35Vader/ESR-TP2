import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
            ConnectionManagerIdentifier connectionManagerIdentifier = new ConnectionManagerIdentifier();

            connectionManagerIdentifier.setConnectionManagers("192.150.20", 1234);
            connectionManagerIdentifier.setConnectionManagers("192.150.21", 1235);
            connectionManagerIdentifier.setConnectionManagers("192.150.22", 1236);
            connectionManagerIdentifier.setConnectionManagers("192.150.30", 4321);

            Node node1 = new Node("192.150.20", 1234, connectionManagerIdentifier, "192.150.30", "230.0.0.1", 9876);
            Node node2 = new Node("192.150.21", 1235, connectionManagerIdentifier, "192.150.30", "230.0.0.1", 9876);
            Node node3 = new Node("192.150.22", 1236, connectionManagerIdentifier, "192.150.30", "230.0.0.1", 9876);

            Bootstraper bootstraper = new Bootstraper("192.150.30",4321,connectionManagerIdentifier);
            String[] ips = {"192.150.20","192.150.21","192.150.22","192.150.30"};

            String[] vizinhos = { "192.150.21,192.150.22",
                                  "192.150.20,192.150.22,192.150.30",
                                  "192.150.20,192.150.21",
                                  "192.150.21"};

            bootstraper.setTopologia(ips,vizinhos);

    }
}