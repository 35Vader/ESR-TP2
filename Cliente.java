import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Cliente {

    // ip do Cliente
    private final String ip;

    // flag de estar a receber stream ou não
    private boolean ReceberStream = false;

    // porta que é a porta do meu node
    private final int porta_do_node_folha;

    private final String ip_do_node_folha;

    // porta do socket udp para enviar strems
    private final int porta_strems;

    // a porta do cliente
    private final int porta;

    ///lock da fila de espera;
    private final ReentrantLock l_fila_de_espera = new ReentrantLock();

    // lock da lista de arvores completas
    private final ReentrantLock l_arvores_completas = new ReentrantLock();

    // ip -> [men]
    private final HashMap<String, ArrayList<String>> fila_de_espera = new HashMap<>();

    // ip_de_quem-eu_quero_enviar em TopDown -> ( arvore_completa, estado de ativação )
    private final HashMap<String, ArvoreEstado> arvores_completas = new HashMap<>();

    public Cliente(String ip, int porta_do_node_folha, String ip_do_node_folha, int porta_strems, int porta){
        this.ip = ip;
        this.porta_do_node_folha = porta_do_node_folha;
        this.ip_do_node_folha = ip_do_node_folha;
        this.porta_strems = porta_strems;
        this.porta = porta;
    }

    private class ArvoreEstado {
        private String arvore;
        private boolean estado;

        // Construtor da classe Par
        public ArvoreEstado(String arvore, boolean estado) {
            this.arvore = arvore;
            this.estado = estado;
        }

        // Métodos getter para obter os valores
        public String getArvore() {
            return arvore;
        }

        public boolean getEstado() {
            return estado;
        }

        // Métodos setter para obter os valores
        public void setArvore(String arvore) {

            this.arvore = arvore;

        }

        public void setEstado( boolean estado) {

            this.estado = estado;
        }
    }
    
    private String ChooseKey(HashMap<String, ArrayList<String>> fila) {
        Set<String> v = new HashSet<>(); // vizinhos que mandaram mensagues
    
        for (String key : fila.keySet()) {
            if (!fila.get(key).isEmpty()) v.add(key);
        }
    
        // Convert the Set to a List
        List<String> myList = new ArrayList<>(v);
    
        // Use Random to generate a random index
        Random random = new Random();
        int randomIndex = random.nextInt(myList.size());
    
        return myList.get(randomIndex);
    
    }

    private void SmartPut(String ip, String mensagem, HashMap<String, ArrayList<String>> fila) {
        ArrayList<String> temp;
        if ((temp = fila.get(ip)) != null) temp.add(mensagem);

        else {
            temp = new ArrayList<>();
            temp.add(mensagem);
            fila.put(ip, temp);
        }

    }

    //recessor geral
    private void servidor() {
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
                            //ip-tipo/mensg
                            mensagem = leitor_vizinho.readLine();
                            // [ip,tipo/mensg]
                            String[] ip_mensg = mensagem.split("-");
                            try {
                                l_fila_de_espera.lock();
                                SmartPut(ip_mensg[0], ip_mensg[1], this.fila_de_espera);
                            } finally {
                                l_fila_de_espera.unlock();
                            }

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                // uma especie de capataz
                new Thread(() -> {
                    try {
                        Socket escritor = ouvinte_mestre.accept();
                        PrintWriter escritor_vizinho = new PrintWriter(escritor.getOutputStream());
                        while (true) {
                            if (!this.fila_de_espera.values().isEmpty()) {
                                Thread t1;
                                String mensagem;
                                String ip;
                                try {
                                    l_fila_de_espera.lock();
                                    ip = ChooseKey(this.fila_de_espera);
                                    mensagem = this.fila_de_espera.get(ip).get(0);
                                    this.fila_de_espera.get(ip).remove(0);
                                } finally {
                                    l_fila_de_espera.unlock();
                                }

                                // [tipo, meng]
                                String[] mensagem_split = mensagem.split("/");

                                switch (mensagem_split[0]) {

                                    case "Arvore":
                                        try {
                                            // Rp tem este ip 121.191.51.101
                                            //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore que o RP escolheu
                                            l_arvores_completas.lock();
                                            ArvoreEstado temp = new ArvoreEstado(mensagem_split[1],false);
                                            this.arvores_completas.put(ip,temp);
                                        } finally {
                                            l_arvores_completas.unlock();
                                        }
                                        break;

                                    case "Stream":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore ativa
                                        try {
                                            l_arvores_completas.lock();
                                            this.arvores_completas.get(ip).setEstado(true);
                                        } finally {
                                            l_arvores_completas.unlock();
                                        }
                                        this.ReceberStream = true;
                                        t1 = new Thread(this::servidor_stream);
                                        t1.start();
                                        break;

                                    case "Atualiza?":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore ativa atualizada totalmente
                                        try {
                                            l_arvores_completas.lock();
                                            this.arvores_completas.get(ip).setArvore(mensagem_split[1]);
                                        } finally {
                                            l_arvores_completas.unlock();
                                        }
                                        escritor_vizinho.println(this.ip + "-ArvoreAtualizada/" + mensagem_split[1]);
                                        break;

                                    case "Atualisa":
                                        escritor_vizinho.println(this.ip + "-Atualizei/" + mensagem_split[1]);
                                        break;

                                    default:
                                        System.out.println("Mensagem inválida");
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // recetor e propagador de strems
    private void servidor_stream() {
        try (DatagramSocket socket = new DatagramSocket(this.porta_strems)) {
            try {
                byte[] receiveData = new byte[1024];
                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);

                    // Converte os bytes recebidos para um DataInputStream
                    ByteArrayInputStream byteStream = new ByteArrayInputStream(receivePacket.getData());
                    DataInputStream dataInputStream = new DataInputStream(byteStream);

                    // Lê os dados do DataInputStream
                    int length = dataInputStream.readInt();
                    byte[] data = new byte[length];
                    dataInputStream.readFully(data);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void QueroStream() throws IOException{

        Socket bootstraper;
        PrintWriter escritor;

        bootstraper = new Socket(ip, porta_do_node_folha);
        escritor = new PrintWriter(bootstraper.getOutputStream(), true);

        escritor.println(this.ip + "-" + "Stream?/");

        try {
            escritor.close();
            bootstraper.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void NaoQueroStream() throws IOException {
        String arvore_a_desativar;

        try {
            l_arvores_completas.lock();
            arvore_a_desativar =  this.arvores_completas.get(ip_do_node_folha).getArvore();
            this.arvores_completas.get(ip_do_node_folha).setEstado(false);
        } finally { l_arvores_completas.unlock();}

        Socket bootstraper;
        PrintWriter escritor;

        bootstraper = new Socket(ip, porta_do_node_folha);
        escritor = new PrintWriter(bootstraper.getOutputStream(), true);
        this.ReceberStream = false;
        escritor.println(this.ip + "-" + "Acabou/" + arvore_a_desativar);

        try {
            escritor.close();
            bootstraper.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

