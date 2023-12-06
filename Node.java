import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

// Momento em que foi pode ter diferenção congestionamento de rede
// Fluxo pode vir de modes diferentes

//mensagens para nós = ip-type/meng

public class Node {
    // ip do Node
    private final String ip;

    // lag se diz se eu estou a stremar ou não
    private boolean Stremar = false;

    // porta do bootstraper
    private final int porta_bootstraper;

    // porta do socket udp para enviar strems
    private final int porta_strems;

    // porta do socket de tcp
    private final int porta;

    //lock das listas de latencias
    private final ReentrantLock l_lantencia = new ReentrantLock();

    // lock da lista de arvores_incompletas de caminhos
    private final ReentrantLock l_mensagem = new ReentrantLock();

    ///lock da fila de espera;
    private final ReentrantLock l_fila_de_espera = new ReentrantLock();

    // lock da lista de vizinhos
    private final ReentrantLock l_vizinhos = new ReentrantLock();

    // lock da lista de vizinhos
    private final ReentrantLock l_vizinhos_udp = new ReentrantLock();

    // lock da lista de vizinhos
    private final ReentrantLock l_arvores_completas = new ReentrantLock();

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

    //ip do que me enviou Arvore? -> arvore incompleta
    private final HashMap<String, String> arvores_incompletas = new HashMap<>();

    // ip vizinhos que quero medir as latencia -> tempo que mandei a mensaguem
    private final HashMap<String, Long> latencia = new HashMap<>();

    // id da arvore-> [ip_de_quem-eu_quero_enviar em BottomUp, arvore_completa]
    private final HashMap<Integer, String[]> arvores_completas = new HashMap<>();

    // ip -> Thread a interromper
    private final HashMap<String, Thread> lista_threads = new HashMap<>();

    // Construtor
    public Node(String ip, int porta, int porta_bootstraper, int porta_strems){

        this.ip = ip;
        this.porta = porta;
        this.porta_bootstraper = porta_bootstraper;
        this.porta_strems = porta_strems;
    }

    public void inicializa() {
        // preparar servidor
        servidor();
    }

    public void PedeVizinhos() throws IOException {
        //primeira fase
        requestVizinhos();

    }

    public void TudoOK(){
        // segunda fase
        okVizinhos();
    }

    private void AtualizaArvores(String ip_quem_me_enviou_top_down, String arvore_atualizada){
        for (Integer i: this.arvores_completas.keySet()){
            if(this.arvores_completas.get(i)[0].equals(ip_quem_me_enviou_top_down)){
                this.arvores_completas.get(i)[1] = arvore_atualizada;
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

    private int mini(Long[] array) {
        // Inicializa o índice mínimo como 0
        int indiceMinimo = 0;

        // Itera sobre o array para encontrar o índice do valor mínimo
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[indiceMinimo]) {
                indiceMinimo = i;
            }
        }
        return indiceMinimo;
    }

    private String ChooseTree() {
        Long[] latencias_totais = new Long[this.arvores_completas.size()];

        for (Integer i : this.arvores_completas.keySet()) {
            latencias_totais[i - 1] = GetLatencia(this.arvores_completas.get(i)[1]);
        }
        return QuemEnviarTopDown( this.arvores_completas.get(mini(latencias_totais))[1]);
    }

    private String QuemEnviarBottomUp(String arvore) {
        String[] caminhos = arvore.split("!");
        String res = "";
        for (String s : caminhos) {

            String[] partes = s.split(",");
            if (partes[0].equals(this.ip)) { res = partes[2];break;
            }
        }
        return res;
    }

    private String QuemEnviarTopDown(String arvore) {
        String[] caminhos = arvore.split("!");
        String res = "";
        for (String s : caminhos) {

            String[] partes = s.split(",");
            if (partes[2].equals(this.ip)) { res = partes[0]; break; }
        }
        return res;
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

        for (String ip: this.vizinhos.keySet()) {
            System.out.println( "o vizinho " + ip + " tem a porta " + this.vizinhos.get(ip));
        }
    }


    private boolean IsEmpty(HashMap<String, ArrayList<String>> emp){
        boolean res = true;
        try {
            l_fila_de_espera.lock();

            for (String s: emp.keySet()) {
                if ( (!emp.get(s).isEmpty()) ) {res = false; break;}
            }
        }finally {l_fila_de_espera.unlock();}

        return res;
    }

    //recessor geral
    private void servidor() {
        // Uma especie de recessionista
        new Thread(() -> {

                // Thread para leitura de mensagens de todos os seus vizinhos
                new Thread(() -> {
                    try {
                        System.out.println("Pronto para receber");
                        // ligação entre um vizinho e 'eu' (eu sou um Node)
                        ServerSocket ouvinte_mestre = new ServerSocket(this.porta);
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
                            leitor_vizinho.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                // uma especie de capataz
                new Thread(() -> {
                    try {
                        System.out.println("Pronto para enviar");
                        while (true) {

                            if (!( IsEmpty (this.fila_de_espera) )) {
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
                                        // digo o meu estado ao vizinho que me enviou
                                        escritor_vizinho(ip,this.ip + "-ok/");
                                        break;

                                    case "ok":
                                        try {
                                            l_ok.lock();
                                            this.estados_de_vizinhos.put(ip, "ok");
                                        } finally {
                                            l_ok.unlock();
                                        }
                                        System.out.println("O meu vizinho " + ip + " está ok!");
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
                                        // envio uma arvores_incompletas metrica para o vizinho que me pediu para medir as métricas
                                        escritor_vizinho(ip, this.ip + "-metrica/" + mensagem_split[1]);
                                        break;

                                    case "metrica":
                                        // parar o timer para o ip, manda mensaguem Arvore para este vizinho com
                                        long tempo_fim = System.currentTimeMillis(); // medição da latencia em roudtrip
                                        long latencia;
                                        String arvore_atualizada;
                                        try {
                                            l_lantencia.lock();
                                            latencia = tempo_fim - this.latencia.get(ip);
                                        } finally {
                                            l_lantencia.unlock();
                                        }

                                        // as metricas atualizadas
                                        try {
                                            l_mensagem.lock();
                                            String arvore = this.arvores_incompletas.get(mensagem_split[1]);
                                            arvore_atualizada = arvore + "!" + this.ip + "," + latencia + "," + ip;
                                        } finally {
                                            l_mensagem.unlock();
                                        }
                                        // propagar o flow para a construção de arvores de forma top down
                                        escritor_vizinho(ip, this.ip + "-Arvore?/" + arvore_atualizada);
                                        break;

                                    case "Arvore?":
                                        // "121.191.51.101 ,10, 121.191.52.101!etc!etc!etc"
                                        // significa que de o Node da esquerda até a Node da direita
                                        // a stream demora 10 milesegundos
                                        try {
                                            l_mensagem.lock();
                                            this.arvores_incompletas.put(ip, mensagem_split[1]);
                                        } finally {
                                            l_mensagem.unlock();
                                        }
                                        metricas(ip);
                                        break;

                                    case "Arvore":
                                        String ip_enviar3;
                                        try {
                                            // Rp tem este ip 121.191.51.101
                                            //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore escolhida pelo RP
                                            l_arvores_completas.lock();
                                            ip_enviar3 = QuemEnviarBottomUp(mensagem_split[1]);
                                            int key = this.arvores_completas.size() + 1;
                                            String[] temp = {ip_enviar3, mensagem_split[1]};
                                            this.arvores_completas.put(key, temp);
                                        } finally {
                                            l_arvores_completas.unlock();
                                        }
                                        sendArvoreAtiva(ip_enviar3,mensagem_split[1]);
                                        break;

                                    case "Stream?":
                                        if (!this.Stremar) {
                                            try {
                                                l_arvores_completas.lock();
                                                if (this.arvores_completas.isEmpty()) {

                                                    try {
                                                        l_mensagem.lock();
                                                       this.arvores_incompletas.put(ip,mensagem_split[1]);
                                                    } finally {
                                                        l_mensagem.unlock();
                                                    }

                                                    metricas(ip);
                                                } else {

                                                    if (this.arvores_completas.size() == 1) {
                                                        String ip_a_enviar = QuemEnviarTopDown(this.arvores_completas.get(1)[1]);
                                                        sendTree(ip_a_enviar);
                                                        System.out.println("Eu "+ this.ip + " vou enviar este ip: " + ip_a_enviar);
                                                    }

                                                    if (this.arvores_completas.size() > 1) {
                                                        String ip_enviar = ChooseTree();
                                                        sendTree(ip_enviar);
                                                        System.out.println("Eu "+ this.ip + "vou enviar este ip: " + ip_enviar);
                                                    }
                                                }
                                            } finally {
                                                l_arvores_completas.unlock();
                                            }
                                        } else {
                                            System.out.println("Hora de fazer multicast");
                                            sendSream(ip,mensagem_split[1]);
                                            Thread.sleep(30);
                                            Thread t1 = new Thread(() -> servidor_stream(ip));
                                            try {
                                                l_thread.lock();
                                                lista_threads.put(ip,t1);
                                            }finally {l_thread.unlock();}
                                            t1.start();
                                        }
                                        break;

                                    case "Stream":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore ativa
                                        this.Stremar = true;

                                        String ip_a_enviar2 = QuemEnviarBottomUp(mensagem_split[1]);

                                        Thread t1 = new Thread(() -> servidor_stream(ip_a_enviar2));
                                        try {
                                            l_thread.lock();
                                            lista_threads.put(ip,t1);
                                        }finally {l_thread.unlock();}
                                        t1.start();

                                        sendSream(ip_a_enviar2,mensagem_split[1]);

                                        Thread t = new Thread( () -> AddArvore(mensagem_split[1],ip) );
                                        t.start();

                                        System.out.println("Eu "+ this.ip + "estou pronto para stremar!!!");
                                        break;
                                    case "Acabou":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore  a desativar
                                        this.Stremar = false;
                                        String ip_a_enviar = QuemEnviarTopDown(mensagem_split[1]);
                                        Thread temp;
                                        try {
                                            l_thread.lock();
                                            temp = lista_threads.get(ip);
                                        }finally {l_thread.unlock();}

                                        temp.interrupt();

                                        sendAcabou(ip_a_enviar,mensagem_split[1]);
                                        break;

                                    case "Atualiza?":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore ativa atualizada até mim
                                        String ip_a_enviar1 = QuemEnviarBottomUp(mensagem_split[1]);
                                        requestLatencia(ip_a_enviar1,mensagem_split[1]);
                                        break;

                                    case "Atualiza":
                                        escritor_vizinho(ip,this.ip + "-Atualizei/" + mensagem_split[1]);
                                        break;

                                    case "Atualizei":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore ativa atualizada até mim imclusive
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
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore ativa atualizada totalmente
                                        try {
                                            // Rp tem este ip 121.191.51.101
                                            //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc"
                                            l_arvores_completas.lock();
                                            AtualizaArvores(ip, mensagem_split[1]);
                                        } finally {
                                            l_arvores_completas.unlock();
                                        }
                                        String ip_enviar4 = QuemEnviarTopDown(mensagem_split[1]);

                                        sendArvoreAtualisada(ip_enviar4,mensagem_split[1]);
                                        break;


                                    default:
                                        System.out.println("Mensagem inválida");
                                        System.out.println(this.ip);
                                }
                            }
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
        }).start();
    }

    // recetor e propagador de strems
    private void servidor_stream(String ip_vizinho) {
        try (DatagramSocket socket = new DatagramSocket(this.porta_strems)) {
            try {
                byte[] receiveData;
                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(receivePacket);

                    // Obter o tamanho real dos dados recebidos
                    int length = receivePacket.getLength();

                    // Criar um novo array apenas com os dados válidos
                    receiveData = Arrays.copyOf(receivePacket.getData(), length);
                    System.out.println("Eu " +this.ip);

                    // Converte os bytes recebidos para um DataInputStream
                    ByteArrayInputStream byteStream = new ByteArrayInputStream(receiveData);
                    DataInputStream dataInputStream = new DataInputStream(byteStream);

                    // Lê os dados do DataInputStream
                    int dataLength = dataInputStream.readInt();
                    byte[] data = new byte[dataLength];
                    //System.out.println(data.length);
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
        System.out.println("eu estou a pedir vizinhos");

        Socket bootstraper;
        PrintWriter escritor;

        bootstraper = new Socket("localhost", porta_bootstraper);
        escritor = new PrintWriter(bootstraper.getOutputStream(), true);

        escritor.println(this.ip + "-" + "Vizinhos/" + this.porta);

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
            }
        }

    // medir o tempo de quanto demora antes de mandar uma mensaguem
    private void metricas(String ip_vizinho_enviou) {

        // verificar se todos os meus vizinhos estão preparados para pode-mos mandar mensaguens
        if (this.estados_de_vizinhos.values().stream().allMatch(value -> value.equals("ok"))) {

            for (String ip : this.vizinhos.keySet()) {

                if (!ip.equals(ip_vizinho_enviou)) {
                    new Thread(() -> {
                        Socket vizinho = null;
                        PrintWriter escritor = null;

                        try {
                            vizinho = new Socket("localhost", this.vizinhos.get(ip));
                            escritor = new PrintWriter(vizinho.getOutputStream(), true);

                            // medir o tempo inicial
                            long tempo_ini = System.currentTimeMillis();
                            escritor.println(this.ip + "-metricas?/" + ip_vizinho_enviou);
                            try {
                                l_lantencia.lock();
                                latencia.put(ip, tempo_ini);
                            } finally {
                                l_lantencia.unlock();
                            }

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
        } else System.out.println("Erro!!!!");
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

    private void sendTree(String ip_do_vizinho_a_enviar) throws IOException {

        Socket vizinho_a_enviar;
        PrintWriter escritor;
        int porta_vizinho;

        porta_vizinho = this.vizinhos.get(ip_do_vizinho_a_enviar);

        vizinho_a_enviar = new Socket("localhost", porta_vizinho);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        escritor.println(this.ip + "-Stream?/");

        try {
            escritor.close();
            vizinho_a_enviar.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendAcabou(String ip_do_vizinho_a_enviar,String arvore_a_desativar) throws IOException {

        Socket vizinho_a_enviar;
        PrintWriter escritor;
        int porta_vizinho = this.vizinhos.get(ip_do_vizinho_a_enviar);

        vizinho_a_enviar = new Socket("localhost", porta_vizinho);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        escritor.println(this.ip + "-Acabou/" + arvore_a_desativar);

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

    private void  sendArvoreAtualisada(String ip_do_vizinho_a_enviar, String arvore_ativa) throws IOException {

        Socket vizinho_a_enviar;
        PrintWriter escritor;
        int porta_vizinho = this.vizinhos.get(ip_do_vizinho_a_enviar);

        vizinho_a_enviar = new Socket("localhost", porta_vizinho);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        escritor.println(this.ip + "-ArvoreAtualizada/" + arvore_ativa);

        try {
            escritor.close();
            vizinho_a_enviar.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void AddArvore(String arvore_nova, String ip){
        try {
            boolean b = false;
            this.l_arvores_completas.lock();
            for (Integer i: this.arvores_completas.keySet()) {
                    if ( this.arvores_completas.get(i)[1].equals(arvore_nova)) {b = true; break;}
            }
            if (!b){
                String[] temp = {ip,arvore_nova}; Integer d = this.arvores_completas.size()+1; this.arvores_completas.put(d,temp);}
        }finally {l_arvores_completas.unlock();}

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
