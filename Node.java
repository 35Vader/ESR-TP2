import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;


public class Node {
    private String ip;

    ///private ReentrantLock vizinhos_lock;

    private int porta_ouve;
    private int porta_escreve;

    private Set<String> vizinhos = new HashSet<>();
    private int porta_bootstraper;

    private Socket escreve;
    private Socket ouve;

    //private InetAddress group;
    //private MulticastSocket multicastSocket;

    // Construtor
    public Node(String ip, int porta_ouve, int porta_escreve , int porta_bootstraper) throws IOException {

        this.ip = ip;
        this.porta_ouve    = porta_ouve;
        this.porta_escreve = porta_escreve;
        this.porta_bootstraper = porta_bootstraper;
        this.escreve = new Socket(ip,porta_escreve);
        this.ouve = new Socket(ip,porta_ouve);

        // Configuração do socket multicast
        ///this.group = InetAddress.getByName(multicastGroup);
        //this.multicastSocket = new MulticastSocket(multicastPort);
        //this.multicastSocket.joinGroup(group);

        // Inicialização do Connection Manager
    }

    public void setVizinhos() throws IOException{
        new Thread(() -> {
            Socket bootstraper = null;
            BufferedReader leitor = null;
            PrintWriter escritor = null;
            String vizinhos;
            try {

                bootstraper = new Socket(ip, porta_bootstraper);
                leitor = new BufferedReader(new InputStreamReader(bootstraper.getInputStream()));
                escritor = new PrintWriter(bootstraper.getOutputStream(), true);

                escritor.println("Vizinhos");

                //while ( leitor.readLine() == null) wait();

                vizinhos = leitor.readLine();

                String[] ips = vizinhos.split(",");
                for (String ip : ips) {
                    this.vizinhos.add(ip);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

}
