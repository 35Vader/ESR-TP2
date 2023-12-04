import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class RP {
    private final String ip;

    // flag se diz se eu estou a stremar ou não
    private boolean Stremar = false;

    // porta do bootstraper
    private final int porta_bootstraper;

    //porrta do servidor
    private  final int  porta_servidor;

    // porta do socket udp para enviar strems
    private final int porta_strems;

    // porta do socket de tcp
    private final int porta;

    //lock das listas de latencias
    private final ReentrantLock l_lantencia = new ReentrantLock();

    // lock da lista de arvores_escolha de caminhos
    private final ReentrantLock l_arvore_escolhas = new ReentrantLock();

    ///lock da fila de espera;
    private final ReentrantLock l_fila_de_espera = new ReentrantLock();

    // lock da lista de vizinhos
    private final ReentrantLock l_vizinhos = new ReentrantLock();

    // lock da lista de vizinhos
    private final ReentrantLock l_vizinhos_udp = new ReentrantLock();

    // lock da lista dos estados dos vizinhos
    private final ReentrantLock l_ok = new ReentrantLock();

    // loock da lista de threads
    private final ReentrantLock l_thread = new ReentrantLock();

    // ip->porta_tcp
    private final HashMap<String, Integer> vizinhos = new HashMap<>();

    //ip->porta_udp
    private final HashMap<String, Integer> vizinhos_udp = new HashMap<>();

    // ip -> [men]
    private final HashMap<String, ArrayList<String>> fila_de_espera = new HashMap<>();

    // ip -> "ok" ou ""
    private final HashMap<String, String> estados_de_vizinhos = new HashMap<>();

    // ip botuum up -> [arvore que ele envio, latencia da arvore]
    private final HashMap<String, ArrayList <ArvoreLatencia>> arvores_escolha = new HashMap<>();

    // ip vizinhos que quero medir as latencia -> tempo que mandei a mensaguem
    private final HashMap<String, Long> latencia = new HashMap<>();

    // ip -> Thread a interromper
    private final HashMap<String, Thread> lista_threads = new HashMap<>();

    // Construtor
    public RP(String ip, int porta, int porta_bootstraper, int porta_servidor, int porta_strems){

        this.ip = ip;
        this.porta = porta;
        this.porta_bootstraper = porta_bootstraper;
        this.porta_servidor = porta_servidor;
        this.porta_strems = porta_strems;
    }

    private static class ArvoreLatencia {
        private String arvore;
        private long latencia;

        // Construtor da classe Par
        public ArvoreLatencia(String arvore, long estado) {
            this.arvore = arvore;
            this.latencia = estado;
        }

        // Métodos getter para obter os valores
        public String getArvore() {
            return arvore;
        }

        public long getLatencia() {
            return latencia;
        }

        // Métodos setter para obter os valores
        public void setArvore(String arvore) {

            this.arvore = arvore;

        }

        public void setLatencia(long latencia) {

            this.latencia = latencia;
        }
    }

    public void inicializa() throws IOException {
        // preparar servidor
        servidor();
        // spleep ??
        //primeira fase
        requestVizinhos();
        // spleep ??
        // segunda fase
        okVizinhos();

    }

    private static String EspelhaInverte(String arvore) {
        String[] groups = arvore.split("!");
        StringBuilder result = new StringBuilder();

        for (String group : groups) {
            String[] elements = group.split(",");
            StringBuilder reversedGroup = new StringBuilder();

            // Reverse the order of elements within the group
            for (int i = elements.length - 1; i >= 0; i--) {
                reversedGroup.append(reverseString(elements[i]));

                if (i > 0) {
                    reversedGroup.append(",");
                }
            }

            result.append(reversedGroup);

            if (!result.toString().equals(groups[groups.length - 1])) {
                result.append("!");
            }
        }

        return result.toString();
    }

    private static String reverseString(String str) {
        return new StringBuilder(str).reverse().toString();
    }


    private void AtualizaArvores(String ip, String arvore_atualizada){

        for ( ArvoreLatencia al : this.arvores_escolha.get(ip) ){
                if(arvore_atualizada.equals(al.getArvore())){
                    al.setArvore(arvore_atualizada);
                }
        }

    }

    private static String joinArray(String[] array) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < array.length; i++) {
            result.append(array[i]);

            if (i < array.length - 1) {
                // Adiciona um ponto de exclamação (!) entre os elementos, exceto no último
                result.append("!");
            }
        }

        return result.toString();
    }

    private String Atualiza(Long novaLatencia, String arvore_a_atualizar){
        String[] caminhos = arvore_a_atualizar.split("!");
        for (String s : caminhos) {

            String[] partes = s.split(",");
            if (partes[0].equals(this.ip)) { partes[1] = novaLatencia.toString();break;}
        }

        return  joinArray(caminhos);
    }

    private Long GetLatencia(String arvore) {
        // Divida a string usando delimitadores
        String[] partes = arvore.split("[,!]");

        long soma = 0;

        // Itere sobre as partes e some os valores numéricos
        for (int i = 1; i < partes.length; i += 3) {
            soma += Long.parseLong(partes[i]);
        }
        return soma;
    }

    private String ChooseTree(String ip) {
        long min = 0;
        ArvoreLatencia arvoreLatencia = null;
        for (ArvoreLatencia al:this.arvores_escolha.get(ip)) {

            if (al.getLatencia() < min) { min = al.getLatencia(); arvoreLatencia = al;}
        }
        return arvoreLatencia.getArvore();
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

    private void SmartPut2(String ip, ArvoreLatencia arvoreLatencia) {
        ArrayList<ArvoreLatencia> temp;
        if ((temp = arvores_escolha.get(ip)) != null) temp.add(arvoreLatencia);

        else {
            temp = new ArrayList<>();
            temp.add(arvoreLatencia);
            arvores_escolha.put(ip, temp);
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

    private void SetVizinhos(String vizinhos) {
        // [121.191.51.101:12341:14321,121.191.52.101:12342:24321]
        String[] ips_portas = vizinhos.split(",");

        for (String ip_porta : ips_portas) {
            // [121.191.51.101, 12341, 14321]
            String[] vizinho = ip_porta.split(":");

            this.vizinhos.put(vizinho[0], Integer.parseInt(vizinho[1]));

            this.vizinhos_udp.put(vizinho[0], Integer.parseInt(vizinho[2]));
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
                        String mensagem;
                        while (true) {
                            Socket ouvinte = ouvinte_mestre.accept();
                            BufferedReader leitor_vizinho = new BufferedReader(new InputStreamReader(ouvinte.getInputStream()));

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
                        while (true) {
                            if (!this.fila_de_espera.values().isEmpty()) {
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

                                    case "ok?":
                                        // digo o meu latencia ao vizinho que me enviou
                                        escritor_vizinho(ip,this.ip + "-ok/");
                                        break;

                                    case "ok":
                                        try {
                                            l_ok.lock();
                                            this.estados_de_vizinhos.put(ip, "ok");
                                        } finally {
                                            l_ok.unlock();
                                        }
                                        break;

                                    case "Vizinhos":
                                        // "121.191.51.101:12341:14321, 121.191.52.101:12342:24321"
                                        try {
                                            l_vizinhos.lock();
                                            l_vizinhos_udp.lock();
                                            SetVizinhos(mensagem_split[1]);
                                        } finally {
                                            l_vizinhos.unlock();
                                            l_vizinhos_udp.unlock();
                                        }
                                        break;

                                    case "metricas?":
                                        // envio uma arvores_escolha metrica para o vizinho que me pediu para medir as métricas
                                        escritor_vizinho(ip, this.ip + "-metrica/" + mensagem_split[1]);
                                        break;

                                    case "Arvore?":
                                        // "121.191.51.101 ,10, 121.191.52.101!etc!etc!etc"
                                        // ip cliente = 121.191.51.101
                                        // significa que de o Node da esquerda até a Node da direita
                                        // a stream demora 10 milesegundos
                                        String arvore_e_i;
                                        try {
                                            l_arvore_escolhas.lock();
                                            // arvore espelhada e invertida
                                            arvore_e_i = EspelhaInverte(mensagem_split[1]);
                                            long latencia_da_arvore = GetLatencia(arvore_e_i);
                                            ArvoreLatencia temp = new ArvoreLatencia(arvore_e_i,latencia_da_arvore);
                                            SmartPut2(ip,temp);
                                        } finally {
                                            l_arvore_escolhas.unlock();
                                        }
                                        sendArvoreAtiva(ip,arvore_e_i);
                                        break;

                                    case "Stream?":
                                        if (!Stremar) {
                                            try {
                                                l_arvore_escolhas.lock();
                                                    if (this.arvores_escolha.get(ip).size() == 1) {
                                                        String bestTree = this.arvores_escolha.get(ip).get(0).getArvore();
                                                        sendSream(ip,bestTree);
                                                        sendSreamServer(ip);
                                                    }

                                                    else{
                                                        String bestTree = ChooseTree(ip);
                                                        sendSream(ip,bestTree);
                                                        sendSreamServer(ip);
                                                    }

                                            } finally {
                                                l_arvore_escolhas.unlock();
                                            }

                                        } else {
                                            Thread t1 = new Thread(() -> servidor_stream(ip));
                                            try {
                                                l_thread.lock();
                                                lista_threads.put(ip,t1);
                                            }finally {l_thread.unlock();}
                                            t1.start();
                                        }
                                        break;

                                    case "Stream":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> ip ativa
                                        this.Stremar = true;
                                        Thread t1 = new Thread(() -> servidor_stream(mensagem_split[1]));
                                        try {
                                            l_thread.lock();
                                            lista_threads.put(ip,t1);
                                        }finally {l_thread.unlock();}
                                        t1.start();
                                        break;

                                    case "Acabou":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> ip  a desativar
                                        this.Stremar = false;
                                        Thread temp;
                                        try {
                                            l_thread.lock();
                                            temp = lista_threads.get(ip);
                                        }finally {l_thread.unlock();}

                                        temp.interrupt();

                                        sendAcabou();
                                        requestLatencia(ip,mensagem_split[1]);
                                        break;

                                    case "Atualizei":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> ip ativa atualizada até mim imclusive
                                        long tempo_fim1 = System.currentTimeMillis();
                                        long latencia1;
                                        try {
                                            l_lantencia.lock();
                                            latencia1 = tempo_fim1 - this.latencia.get(ip);
                                        } finally {
                                            l_lantencia.unlock();
                                        }
                                        String arvore_atualizada1 = Atualiza(latencia1, mensagem_split[1]);
                                        escritor_vizinho(ip,this.ip + "-Atualiza?/" + arvore_atualizada1);
                                        break;

                                    case "ArvoreAtualizada":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> ip ativa atualizada totalmente
                                        try {
                                            // Rp tem este ip 121.191.51.101
                                            //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc"
                                            l_arvore_escolhas.lock();
                                            AtualizaArvores(ip, mensagem_split[1]);
                                        } finally {
                                            l_arvore_escolhas.unlock();
                                        }
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
    private void servidor_stream(String ip_vizinho) {
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
                    DatagramPacket sendPacket = new DatagramPacket(data, data.length, InetAddress.getByName("localhost"), this.vizinhos_udp.get(ip_vizinho));
                    socket.send(sendPacket);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    // primeira fase defenir os vizinhos
    private void requestVizinhos() throws IOException {

        Socket bootstraper;
        PrintWriter escritor;

        bootstraper = new Socket("localhost", porta_bootstraper);
        escritor = new PrintWriter(bootstraper.getOutputStream(), true);

        escritor.println(this.ip + "-" + "Vizinhos/");

        try {
            escritor.close();
            bootstraper.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //fase para ver se os vizinhos estão ok
    private void okVizinhos() {

        for (String ip : vizinhos.keySet()) {

            new Thread(() -> {
                Socket vizinho = null;
                PrintWriter escritor = null;

                try {
                    vizinho = new Socket("localhost", this.vizinhos.get(ip));
                    escritor = new PrintWriter(vizinho.getOutputStream(), true);

                    escritor.println(this.ip + "-ok?/");

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

    private void sendSreamServer( String ip_a_enviar_me) throws IOException {

        Socket vizinho_a_enviar;
        PrintWriter escritor;

        vizinho_a_enviar = new Socket("localhost", this.porta_servidor);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        escritor.println(this.ip + "-SendStream/" + ip_a_enviar_me);

        try {
            escritor.close();
            vizinho_a_enviar.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private  void  escritor_vizinho(String ip_do_vizinho_a_enviar, String mensagem)throws IOException{

        Socket vizinho_a_enviar;
        PrintWriter escritor;
        int porta_vizinho;

        porta_vizinho = this.vizinhos.get(ip_do_vizinho_a_enviar);

        vizinho_a_enviar = new Socket("localhost", porta_vizinho);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        escritor.println(mensagem);

        try {
            escritor.close();
            vizinho_a_enviar.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendAcabou() throws IOException {

        Socket vizinho_a_enviar;
        PrintWriter escritor;

        vizinho_a_enviar = new Socket("localhost", this.porta_servidor);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        escritor.println(this.ip + "-Acabou/");

        try {
            escritor.close();
            vizinho_a_enviar.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendSream(String ip_do_vizinho_a_enviar,String arvore_a_desativar) throws IOException {

        Socket vizinho_a_enviar;
        PrintWriter escritor;
        int porta_vizinho= this.vizinhos.get(ip_do_vizinho_a_enviar);

        vizinho_a_enviar = new Socket("localhost", porta_vizinho);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        escritor.println(this.ip + "-Stream/" + arvore_a_desativar);

        try {
            escritor.close();
            vizinho_a_enviar.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendArvoreAtiva(String ip_do_vizinho_a_enviar, String arvore_ativa) throws IOException {

        Socket vizinho_a_enviar;
        PrintWriter escritor;
        int porta_vizinho= this.vizinhos.get(ip_do_vizinho_a_enviar);

        vizinho_a_enviar = new Socket("localhost", porta_vizinho);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        escritor.println(this.ip + "-Arvore/" + arvore_ativa);

        try {
            escritor.close();
            vizinho_a_enviar.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void requestLatencia(String ip_do_vizinho_a_enviar, String arvore_a_atualizar) throws IOException {

        Socket vizinho_a_enviar;
        PrintWriter escritor;
        int porta_vizinho = this.vizinhos.get(ip_do_vizinho_a_enviar);

        vizinho_a_enviar = new Socket("localhost", porta_vizinho);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        // medir o tempo inicial
        long tempo_ini = System.currentTimeMillis();

        escritor.println(this.ip + "-Atualiza/" + arvore_a_atualizar);

        try {
            l_lantencia.lock();
            latencia.put(ip, tempo_ini);
        } finally {
            l_lantencia.unlock();
        }

        try {
            escritor.close();
            vizinho_a_enviar.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}


