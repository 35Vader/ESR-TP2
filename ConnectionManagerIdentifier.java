import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class ConnectionManagerIdentifier {

    private HashMap<String,ConnectionManager> ConnectionManagerIdentifierList = new HashMap<>();

    public ConnectionManager getConnectionManager(String ip){
        return this.ConnectionManagerIdentifierList.get(ip);
    }

    public void setConnectionManagers(String ip, ConnectionManager c){
        this.ConnectionManagerIdentifierList.put(ip,c);
    }

    public void setConnectionManagers(String ip, Integer porta) throws IOException {
        Socket s = new Socket(ip,porta);
        ConnectionManager c = ConnectionManager.start(s);
        this.ConnectionManagerIdentifierList.put(ip,c);
    }

    public void closeConecionNode( String ip) throws IOException {
        this.ConnectionManagerIdentifierList.get(ip).close();}

    public void closeEverything() throws IOException {
        for (String s: ConnectionManagerIdentifierList.keySet()) {
            this.ConnectionManagerIdentifierList.get(s).close();
        }
    }
}
