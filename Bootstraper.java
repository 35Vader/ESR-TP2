import java.io.IOException;
import java.util.*;

public class Bootstraper {

    private String ip;
    private int porta;
    private Set<String> vizinhos = new HashSet<>(); // ip dos vizinhos
    private HashMap<String, String> topologia = new HashMap<>(); // ip -> "ip,pi,ip,ip"
    private ConnectionManagerIdentifier connectionManagerIdentifier;

    // Node 1,      ip = 121.191.51.101, porta = 12341
    // Node 2,      ip = 122.192.52.102, porta = 12342
    // Node 3,      ip = 123.193.53.103, porta = 12343
    // Bootstraper, ip = 122.192.52.200, porta = 54321

    public Bootstraper(String ip, Integer porta, ConnectionManagerIdentifier connectionManagerIdentifier) throws IOException {
        this.ip = ip;
        this.porta = porta;
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

    public void setVizinhos(String[] vizinhos) {
        for (String vizinho: vizinhos) {
            this.vizinhos.add(vizinho);
        }
    }

    public void ler(String ip) throws IOException {

        if(is_vizinho(ip) == true) connectionManagerIdentifier.getConnectionManager(ip).read();

        else System.out.println("Não é vizinho!!!");
    }

    public void escrever(String ip, String tipo, String mensagem) throws IOException {
        if(is_vizinho(ip) == true) connectionManagerIdentifier.getConnectionManager(ip).send(tipo, mensagem);

        else System.out.println("Não é vizinho!!!");
    }

    public String receber(String ip) throws InterruptedException {
        String s;

        connectionManagerIdentifier.getConnectionManager(ip).receive("vizinhos");
        s = this.topologia.get(ip);

        return s;
    }

    public String receber(String ip, String tipo) throws InterruptedException {
        String s;
        if(is_vizinho(ip) == true) s = connectionManagerIdentifier.getConnectionManager(ip).receive(tipo);

        else s =  "Não é vizinho!!!";

        return s;
    }
}
