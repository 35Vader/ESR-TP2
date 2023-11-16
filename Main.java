import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
            ConnectionManagerIdentifier connectionManagerIdentifier = new ConnectionManagerIdentifier();
            connectionManagerIdentifier.setConnectionManagers("192.150.20", 1234);
            connectionManagerIdentifier.setConnectionManagers("192.150.21", 1235);
            connectionManagerIdentifier.setConnectionManagers("192.150.22", 1236);

            connectionManagerIdentifier.setConnectionManagers("192.150.30", 4321);
            // Bootstraper bootstraper = 
            //Node node1 = new Node("192.150.20", 1234, connectionManagerIdentifier, "192.150.30", "230.0.0.1", 9876);
    }
}