import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

//mensagens para nós = ip-type|meng

public class Node {
    private  final String ip;

    ///private ReentrantLock vizinhos_lock;
    private final ReentrantLock l_fila_de_espera = new ReentrantLock();

    // lock da lista de vizinhos
    private final ReentrantLock l_vizinhos = new ReentrantLock();

    private final int porta;
    // ip->porta
    private final HashMap<String,Integer> vizinhos = new HashMap<>();
    private final int porta_bootstraper;
    private final HashMap < String, ArrayList<String> > fila_de_espera = new HashMap<>(); // ip -> [men]

    // Construtor
    public Node(String ip, int porta, int porta_bootstraper) throws IOException {

        this.ip = ip;
        this.porta = porta;
        this.porta_bootstraper = porta_bootstraper;
    }

    private void SmartPut(String ip, String mensagem, HashMap<String,ArrayList<String>> fila){
        ArrayList<String> temp;
        if((temp = fila.get(ip)) != null) temp.add(mensagem);

        else{
            temp = new ArrayList<String>();
            temp.add(mensagem);
            fila.put(ip,temp);
        }

    }
    private String ChooseKey(HashMap<String,ArrayList<String>> fila){
        Set<String> v = new HashSet<>(); // vizinhos que mandaram mensagues

        for (String key: fila.keySet() ) {
                if(!fila.get(key).isEmpty()) v.add(key);
        }

        // Convert the Set to a List
        List<String> myList = new ArrayList<>(v);

        // Use Random to generate a random index
        Random random = new Random();
        int randomIndex = random.nextInt(myList.size());

        return myList.get(randomIndex);

    }

    private void SetVizinhos(String vizinhos) {
        // [121.191.51.101:12341,121.191.52.101:12342]
        String[] ips_portas = vizinhos.split(",");

        for (String ip_porta : ips_portas) {
            // [121.191.51.101, 12341]
            String[] vizinho = ip_porta.split(":");

            this.vizinhos.put(vizinho[0], Integer.parseInt(vizinho[1]));
        }
    }

    //recessor geral
    public void servidor(){
        // Uma especie de recessionista
        new Thread(() -> {
                try (ServerSocket ouvinte_mestre = new ServerSocket(this.porta)) {
                    // ligação entre um vizinho e 'eu' (eu sou um Node)

                    // Thread para leitura de mensagens de todos os seus vizinhos
                    new Thread(() -> {
                        try {
                            Socket ouvinte = ouvinte_mestre.accept();
                            BufferedReader leitor_vizinho = new BufferedReader(new InputStreamReader(ouvinte.getInputStream()));
                            String mensagem;
                            while (true) {
                                //ip-tipo|mensg
                                if ( (mensagem = leitor_vizinho.readLine()) != null){
                                    // [ip,tipo|mensg]
                                    String [] ip_mensg = mensagem.split("-");
                                    try {
                                        l_fila_de_espera.lock();
                                        SmartPut(ip_mensg[0], ip_mensg[1],this.fila_de_espera);
                                    }
                                    finally {
                                        l_fila_de_espera.unlock();}
                                    }
                                }
                            }
                        catch (IOException e) {e.printStackTrace();}


                    }).start();

                    // uma especie de capataz
                    new Thread(() -> {
                        try {
                            Socket escritor = ouvinte_mestre.accept();
                            PrintWriter escritor_vizinho = new PrintWriter(escritor.getOutputStream());
                            while (true) {
                                if (!this.fila_de_espera.values().isEmpty()) {
                                    String mensagem;
                                    try {
                                        l_fila_de_espera.lock();
                                        String key = ChooseKey(this.fila_de_espera);
                                        mensagem = this.fila_de_espera.get(key).get(0);
                                        this.fila_de_espera.get(key).remove(0);
                                    } finally {
                                        l_fila_de_espera.unlock();}

                                    // [tipo, meng]
                                    String [] mensagem_split = mensagem.split("|");

                                    switch (mensagem_split[0]) {
                                        case "ok?":
                                            escritor_vizinho.println("ok");
                                            break;

                                        case "Vizinhos":
                                            // "121.191.51.101:12341,121.191.52.101:12342"
                                            try {
                                               l_vizinhos.lock();
                                                SetVizinhos(mensagem_split[1]);
                                            } finally{l_vizinhos.unlock();}

                                        case "stream":
                                            break;

                                        default:
                                            System.out.println("Mensagem inválida");
                                    }
                                }
                            }
                        } catch (IOException e) {e.printStackTrace();}
                    }).start();

                } catch (IOException e) { e.printStackTrace(); }});
    }



    // primeira fase defenir os vizinhos
    public void setVizinhos2() throws IOException{

        new Thread(() -> {
            Socket bootstraper = null;
            PrintWriter escritor = null;

            try {
                bootstraper = new Socket(ip, porta_bootstraper);
                escritor = new PrintWriter(bootstraper.getOutputStream(), true);

                escritor.println(this.ip+ "-" + "Vizinhos|");

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
