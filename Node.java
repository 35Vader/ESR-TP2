import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Node {

    private String ip;
    private int porta;
    private Set<String> vizinhos = new HashSet<>(); // ip
    private String ip_bootstraper;
    private ConnectionManagerIdentifier connectionManagerIdentifier;

    public Node(String ip, Integer porta, ConnectionManagerIdentifier connectionManagerIdentifier, String ip_bootstraper) throws IOException {
        this.ip = ip;
        this.porta = porta;
        this.ip_bootstraper = ip_bootstraper;
        this.connectionManagerIdentifier = connectionManagerIdentifier;
        connectionManagerIdentifier.setConnectionManagers(this.ip,this.porta);
    }
    private boolean is_vizinho(String ip){
        boolean a = false;
        for (String ips: this.vizinhos) {
            if (ips.equals(ip)) {a = true; break;}
        }
        return a;
    }
    public void ler(String ip) throws IOException {

        if(is_vizinho(ip) == true) connectionManagerIdentifier.getConnectionManager(ip).read();

        else System.out.println("Não é vizinho!!!");
    }

    public void ler_bootstraper() throws IOException {
        connectionManagerIdentifier.getConnectionManager(this.ip_bootstraper).read();
    }

    public void escrever(String ip, String tipo, String mensagem) throws IOException {
        if(is_vizinho(ip) == true) connectionManagerIdentifier.getConnectionManager(ip).send(tipo, mensagem);

        else System.out.println("Não é vizinho!!!");
    }

    public void escrever_bootstraper() throws IOException {
        connectionManagerIdentifier.getConnectionManager(this.ip_bootstraper).send("vizinhos", "vizinhos");
    }

    public String receber(String ip, String tipo) throws InterruptedException {
        String s;
        if(is_vizinho(ip) == true) s = connectionManagerIdentifier.getConnectionManager(ip).receive(tipo);

        else s =  "Não é vizinho!!!";

        return s;
    }

    // ip porta, ip porta,...
    // vizinhos = "121.191.51.101,121.191.51.101,121.191.51.101"
    public void receber_bootstraper() throws InterruptedException, IOException {

        String s = connectionManagerIdentifier.getConnectionManager(this.ip_bootstraper).receive("vizinhos");
        // [ip, ip, ...]
        String[] ips = s.split(",");

        for (String ip : ips) vizinhos.add(ip);
    }
}

