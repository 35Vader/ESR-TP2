import java.io.IOException;
import java.util.HashMap;

public class ConnectionManagerIdentifier {

    private HashMap<String,ConnectionManager> ConnectionManagerIdentifierList;

    public ConnectionManager getConnectionManager(String ip){
        return this.ConnectionManagerIdentifierList.get(ip);
    }

    public void setConnectionManagers(String ip, ConnectionManager c){
        this.ConnectionManagerIdentifierList.put(ip,c);
    }

    public void closeEverything() throws IOException {
        for (String s: ConnectionManagerIdentifierList.keySet()) {
            this.ConnectionManagerIdentifierList.get(s).close();
        }
    }
}
