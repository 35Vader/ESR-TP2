import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class Node<mensaguem> {

    private String ip;
    private int porta;
    private HashMap<String, ConnectionManager> vizinhos;
    private ConnectionManager connectionManager_bootstraper;

    public Node(Socket s, String ip, Integer porta) throws IOException {
        this.ip = ip;
        this.connectionManager_bootstraper = new ConnectionManager(s);
        this.porta = porta;
    }

    public void ler(String ip) throws IOException {
        vizinhos.get(ip).read();
    }

    public void ler_bootstraper() throws IOException{
        connectionManager_bootstraper.read();
    }

    public void escrever(String ip, String tipo, String mensagem) throws IOException {
        vizinhos.get(ip).send(tipo,mensagem);
    }

    public void escrever_bootstraper(String tipo, String mensagem) throws IOException {
        connectionManager_bootstraper.send(tipo,mensagem);
    }

    public String receber(String ip, String tipo) throws InterruptedException {
        String s = vizinhos.get(ip).receive(tipo);

        return s;
    }

    public String receber_bootstraper(String tipo) throws InterruptedException {
        String s = connectionManager_bootstraper.receive(tipo);
        return s;
    }
}


