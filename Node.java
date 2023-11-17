import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;


public class Node {
    private String ip;

    ///private ReentrantLock vizinhos_lock;

    private int porta_ouve;
    private int porta_escreve;

    private HashMap<String,Integer> vizinhos = new HashMap<>();
    private int porta_bootstraper;
    private ArrayList<String> fila_de_espera;

    // Construtor
    public Node(String ip, int porta_ouve, int porta_escreve , int porta_bootstraper) throws IOException {

        this.ip = ip;
        this.porta_ouve    = porta_ouve;
        this.porta_escreve = porta_escreve;
        this.porta_bootstraper = porta_bootstraper;
    }
    //recessor geral
    public void ressecionista(){
        // Inicie o servidor do Node
        new Thread(() -> {
                try (ServerSocket ouvinte_mestre = new ServerSocket(this.porta_ouve)) {
                    System.out.println("Servidor iniciado na porta " + this.porta_ouve);

                    // Thread para leitura de mensagens do console (entrada)
                    new Thread(() -> {
                        try {
                            Socket ouvinte = ouvinte_mestre.accept();
                            BufferedReader leitor_vizinho = new BufferedReader(new InputStreamReader(ouvinte.getInputStream()));
                            String mensagem;
                            while (true) {
                                if ( (mensagem = leitor_vizinho.readLine()) != null) this.fila_de_espera.add(mensagem);
                                }
                            } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } catch (IOException e) {
                    e.printStackTrace(); }});
    }



    // primeira fase defenir os vizinhos
    public void setVizinhos() throws IOException{
        new Thread(() -> {
            Socket bootstraper = null;
            BufferedReader leitor = null;

            try {
                bootstraper = new Socket(ip, porta_bootstraper);
                leitor = new BufferedReader(new InputStreamReader(bootstraper.getInputStream()));

                while (leitor.readLine() == null);

                // 121.191.51.101:12341,121.191.52.101:12342
                String vizinhos = leitor.readLine();

                // [121.191.51.101:12341,121.191.52.101:12342]
                String[] ips_portas = vizinhos.split(",");

                for (String ip_porta : ips_portas) {
                    // [121.191.51.101, 12341]
                    String[] vizinho = ip_porta.split(":");

                    this.vizinhos.put (vizinho[0], Integer.parseInt(vizinho[1]) );
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (leitor != null) leitor.close();
                    if (bootstraper != null) bootstraper.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            Socket bootstraper = null;
            PrintWriter escritor = null;

            try {
                bootstraper = new Socket(ip, porta_bootstraper);
                escritor = new PrintWriter(bootstraper.getOutputStream(), true);

                escritor.println("Vizinhos," + this.ip);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (escritor != null) escritor.close();
                    if (bootstraper != null) bootstraper.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    //segunda fase
    public void okVizinhos(){
        for ( String ip: vizinhos.keySet() ) {
            new Thread(() -> {
                Socket vizinho = null;
                BufferedReader leitor = null;

                try {
                    vizinho = new Socket(this.ip, this.vizinhos.get(ip));
                    leitor = new BufferedReader(new InputStreamReader(vizinho.getInputStream()));

                    while (leitor.readLine() == null);

                    // ok,ip
                    String mens = leitor.readLine();


                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (leitor != null) leitor.close();
                        if (vizinho != null) vizinho.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            new Thread(() -> {
                Socket vizinho = null;
                PrintWriter escritor = null;

                try {
                    vizinho = new Socket(this.ip, this.vizinhos.get(ip));
                    escritor = new PrintWriter(vizinho.getOutputStream(), true);

                    escritor.println("ok?," + this.ip);

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (escritor != null) escritor.close();
                        if (vizinho != null) vizinho.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

}
