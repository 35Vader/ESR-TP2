import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class Node {

    private String ip;
    private int porta;
    private HashMap<String, ConnectionManager> vizinhos;
    private ConnectionManager connectionManager_bootstraper;

    public Node(Socket s, String ip, Integer porta) throws IOException {
        this.ip = ip;
        this.connectionManager_bootstraper = new ConnectionManager(s);
        this.connectionManager_bootstraper.isOpen();
        this.porta = porta;
    }

    public void ler(String ip) throws IOException {
        vizinhos.get(ip).read();
    }

    public void ler_bootstraper() throws IOException {
        connectionManager_bootstraper.read();
    }

    public void escrever(String ip, String tipo, String mensagem) throws IOException {
        vizinhos.get(ip).send(tipo, mensagem);
    }

    public void escrever_bootstraper(String tipo, String mensagem) throws IOException {
        connectionManager_bootstraper.send(tipo, mensagem);
    }

    public String receber(String ip, String tipo) throws InterruptedException {
        String s = vizinhos.get(ip).receive(tipo);

        return s;
    }
    // ip porta, ip porta,...
    // vizinhos = "121.191.51.101 1234, 121.191.51.101 1560, 121.191.51.101 9990"

    public void receber_bootstraper() throws InterruptedException, IOException {

        String s = connectionManager_bootstraper.receive("vizinhos");
        // [ip porta, ip porta, ...]
        String[] ipPortas = s.split(",");
        for (String ipPorta : ipPortas) {
            //[ip,porta]
            String[] ip_e_porta = ipPorta.split(" ");
            // porta
            Integer porta = Integer.parseInt(ip_e_porta[1]);

            Socket socket = new Socket(ip_e_porta[0],porta);

            ConnectionManager connectionManager_vizinho = ConnectionManager.start(socket);

            this.vizinhos.put(ip_e_porta[0],connectionManager_vizinho);
        }
    }
}

