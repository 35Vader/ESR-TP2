import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Bootstraper {

    private String ip;
    private int porta;
    private HashMap<String, ConnectionManager> vizinhos;
    private HashMap <String, ArrayList<String> > topologia;

    public void ler(String ip) throws IOException {
        vizinhos.get(ip).read();
    }

    public void escrever(String ip, String tipo, String mensagem) throws IOException {
        vizinhos.get(ip).send(tipo,mensagem);
    }

    public String receber(String ip, String tipo) throws InterruptedException {
        String s = vizinhos.get(ip).receive(tipo);
        if (s == "Vizinhos")....
        return s;
    }
}
