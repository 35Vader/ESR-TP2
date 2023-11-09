import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

public class Bootstraper {

    private String ip;
    private int porta;
    private HashMap<String, ConnectionManager> vizinhos;
    private HashMap <String, ArrayList<Integer> > topologia;
    private Queue<String> ips;

    // Node 1,      ip = 121.191.51.101, porta = 12341
    // Node 2,      ip = 122.192.52.102, porta = 12342
    // Node 3,      ip = 123.193.53.103, porta = 12343
    // Bootstraper, ip = 122.192.52.200, porta = 54321

    public void ler(String ip) throws IOException {
        vizinhos.get(ip).read();
    }

    // escrever para os vizinhos
    public void escrever(String ip, String tipo, String mensagem) throws IOException {
        vizinhos.get(ip).send(tipo,mensagem);
    }

    // só recebe mensagens dos vizinhos
    public String receber(String ip, String tipo) throws InterruptedException {
        String s = vizinhos.get(ip).receive(tipo);
        return s;
    }

    public void escrever_vizinhos(String ip, String tipo) throws IOException {

    }
}
