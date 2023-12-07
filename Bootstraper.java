import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

// Momento em que foi pode ter diferenção congestionamento de rede
// Fluxo pode vir de modes diferentes

//mensagens para nós = ip-type/meng

public class Bootstraper {
    // ip do Node
    private final String ip;

    // flag se diz se eu estou a stremar ou não
    private boolean Stremar = false;

    // porta do socket de tcp
    private final int porta;

    private final  int porta_strems;

    // lock da topologia tcp
    private final ReentrantLock l_topologia = new ReentrantLock();

    // lock da topologia tcp
    private final ReentrantLock l_topologia_udp = new ReentrantLock();

    //lock das listas de latencias
    private final ReentrantLock l_lantencia = new ReentrantLock();

    // lock da lista de arvores_incompletas de caminhos
    private final ReentrantLock l_mensagem = new ReentrantLock();

    //lock da lista das arvores ativas
    private final ReentrantLock l_arvores_ativas = new ReentrantLock();

    // lock da lista de vizinhos em "cima de mim" a receber stream
    private  final ReentrantLock l_ativos = new ReentrantLock();

    ///lock da fila de espera;
    private final ReentrantLock l_fila_de_espera = new ReentrantLock();

    // lock da lista de vizinhos
    private final ReentrantLock l_arvores_completas = new ReentrantLock();

    // lock da lista dos estados dos vizinhos
    private final ReentrantLock l_ok = new ReentrantLock();

    // ip->porta_tcp
    private final HashMap<String, Integer> vizinhos = new HashMap<>();

    // loock da lista de threads
    private final ReentrantLock l_thread = new ReentrantLock();

    //ip->porta_udp
    private final HashMap<String, Integer> vizinhos_udp = new HashMap<>();

    // ip -> vizinhos dele
    private final HashMap<String, HashMap<String,Integer>> topologia = new HashMap<>();

    // ip -> vizinhos dele mas com porta udp
    private final HashMap<String, HashMap<String,Integer>> topologia_udp = new HashMap<>();

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

    // ip a cima de mim -> bool
    private final HashMap <String,Boolean> lista_estados = new HashMap<>();

    // arvore -> estado
    private final HashMap<String,Boolean> lista_arvores_ativas = new HashMap<>();

    // Construtor
    public Bootstraper(String ip, int porta, int porta_strems){

        this.ip = ip;
        this.porta = porta;
        this.porta_strems = porta_strems;
    }


    public void setTopologia(String ip,HashMap<String,Integer> v_tcp, HashMap<String,Integer> v_udp){
        this.topologia.put(ip,v_tcp);
        this.topologia_udp.put(ip,v_udp);
    }

    public void setvizinhos(HashMap<String,Integer> vizinhos, HashMap<String,Integer> vizinhos_udp){
        for (String s :vizinhos.keySet()) {
                this.vizinhos.put(s,vizinhos.get(s));
        }

        for (String s :vizinhos_udp.keySet()) {
            this.vizinhos_udp.put(s,vizinhos_udp.get(s));
        }

    }

    public void inicializa() throws IOException {
        // preparar servidor
        servidor();
        //sleep...
        //okVizinhos();
    }

    public void TudoOK(){
        // segunda fase
        okVizinhos();
    }

    private String VizinhosMaker(HashMap<String, Integer> topologia, HashMap<String, Integer> topologia_udp) throws NullPointerException{
        // "121.191.51.101:12341:14321, 121.191.52.101:12342:24321"
        StringBuilder vizinhos = new StringBuilder();
        int i = 0;
        for (String s: topologia.keySet()) {
            if(i < topologia.size()) vizinhos.append(s).append(":").append(topologia.get(s)).append(":").append(topologia_udp.get(s)).append(",");

            else vizinhos.append(s).append(":").append(topologia.get(s)).append(":").append(topologia_udp.get(s));

            i++;
        }

        return vizinhos.toString();
    }


    private void AtualizaArvores(String ip_quem_me_enviou_top_down, String arvore_atualizada){
        for (Integer i: this.arvores_completas.keySet()){
            if(this.arvores_completas.get(i)[0].equals(ip_quem_me_enviou_top_down)){
                this.arvores_completas.get(i)[1] = arvore_atualizada;
            }

        }
    }

    private String ArvoreMulticast(String novoInput) {
        String res = "";

        for (String arvore : this.lista_arvores_ativas.keySet()) {
            if (this.lista_arvores_ativas.get(arvore)) {
                System.out.println(arvore);
                String[] caminhos = arvore.split("!");

                // Encontrar o argumento igual ao último argumento do novoInput
                String ultimoArgumentoNovoInput = novoInput.split(",")[2];
                int indiceUltimoArgumento = -1;
                for (int i = caminhos.length - 1; i >= 0; i--) {
                    if (caminhos[i].endsWith(ultimoArgumentoNovoInput)) {
                        indiceUltimoArgumento = i;
                        break;
                    }
                }

                if (indiceUltimoArgumento != -1) {
                    // Adicionar caminhos até o argumento encontrado
                    for (int i = 0; i <= indiceUltimoArgumento; i++) {
                        res += caminhos[i] + "!";
                    }

                    // Adicionar novoInput invertido após o argumento
                    String inputInvertido = novoInput.split(",")[2] + "," +
                            novoInput.split(",")[1] + "," +
                            novoInput.split(",")[0];
                    res += inputInvertido;
                }

                break;
            }
        }

        return res;
    }

    private String Atualiza(Long novaLatencia, String arvore_a_atualizar) {
        String[] caminhos = arvore_a_atualizar.split("!");
        String res = "";
        int i = 0;
        for (String s : caminhos) {

            String[] partes = s.split(",");
            if (partes[0].equals(this.ip)) res += partes[0] + "," + novaLatencia.toString() + "," + partes[2] + "!";

            else {
                if( i < caminhos.length - 1 || i == 0) res += s + "!";
                else res += s;
            }
            i++;

        }
        return res;
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

    private void SmartPut(String ip,Thread t) {
        if ((this.lista_threads.get(ip)) == null) this.lista_threads.put(ip,t);

        else { this.lista_threads.remove(ip); this.lista_threads.put(ip,t);}

    }

    private void SmartPut(String ip, Boolean b) {
        if ((this.lista_estados.get(ip)) == null) this.lista_estados.put(ip,b);

        else { this.lista_estados.remove(ip); this.lista_estados.put(ip,b);}

    }

    private void SmartPutArvore(String arvore, Boolean b) {
        if ((this.lista_arvores_ativas.get(arvore)) == null) this.lista_arvores_ativas.put(arvore,b);

        else { this.lista_estados.remove(arvore); this.lista_estados.put(arvore,b);}

    }

    private String ChooseTree() {
        Long[] latencias_totais = new Long[this.arvores_completas.size()];

        for (Integer i : this.arvores_completas.keySet()) {
            latencias_totais[i - 1] = GetLatencia(this.arvores_completas.get(i)[1]);
        }
        return this.arvores_completas.get(mini(latencias_totais))[0];
    }

    public String QuemEnviarBottomUp(String arvore) {
        String[] caminhos = arvore.split("!");
        String res = "";
        for (String s : caminhos) {

            String[] partes = s.split(",");
            if (partes[0].equals(this.ip)) { res = partes[2];break;
            }
        }
        return res;
    }

    public String QuemEnviarTopDown(String arvore) {
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

    private boolean IsEmpty(HashMap<String, ArrayList<String>> emp){
        boolean res = true;
        try {
            l_fila_de_espera.lock();

            for (String s: emp.keySet()) {
                if ( (emp.get(s).isEmpty() == false) ) {res = false; break;}
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
                        String mensagem;
                        ServerSocket ouvinte_mestre = new ServerSocket(this.porta);
                            // ligação entre um vizinho e 'eu' (eu sou um Node)
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
                        while (true) {
                            if ( !( IsEmpty (this.fila_de_espera) ) ){
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
                                        break;

                                    case "Vizinhos":
                                        // "121.191.51.101:12341:14321, 121.191.52.101:12342:24321"
                                        String vizinhos;
                                        try {
                                            l_topologia.lock();
                                            l_topologia_udp.lock();
                                           vizinhos = VizinhosMaker( this.topologia.get(ip), this.topologia_udp.get(ip));

                                        } finally {
                                            l_topologia_udp.unlock();
                                            l_topologia.unlock();
                                        }

                                        escritor_vizinho(Integer.parseInt(mensagem_split[1]), this.ip+ "-Vizinhos/" + vizinhos);

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
                                            this.Stremar = true;
                                            String Arvoremulticast;
                                            String ip_novo;
                                            try {
                                                l_ativos.lock();
                                                l_arvores_ativas.lock();
                                                l_arvores_completas.lock();

                                                Arvoremulticast =  ArvoreMulticast(mensagem_split[1]);
                                                ip_novo = QuemEnviarBottomUp(Arvoremulticast);
                                                SmartPut(ip_novo,true);
                                                // colucar a Arvoremulticast no arvores completas e no arvores ativas faz um SmartPut para as arvores ativas
                                                SmartPutArvore(Arvoremulticast,true);

                                                String[] yy = {ip_novo, Arvoremulticast,};
                                                this.arvores_completas.put(this.arvores_completas.size() + 1,yy);

                                            }finally {
                                                l_arvores_completas.unlock();
                                                l_arvores_ativas.unlock();
                                                l_ativos.unlock();}

                                            sendSream(ip_novo, Arvoremulticast);
                                            Thread.sleep(30);
                                            Thread t1 = new Thread(() -> {
                                                try {
                                                    servidor_stream(ip_novo);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                            try {
                                                l_thread.lock();
                                                SmartPut(ip,t1);
                                            }finally {l_thread.unlock();}
                                            t1.start();
                                        }
                                        break;

                                    case "Stream":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore ativa
                                        this.Stremar = true;
                                        String ip_a_enviar2 = QuemEnviarBottomUp(mensagem_split[1]);

                                        try {
                                            l_ativos.lock();

                                            SmartPut(ip_a_enviar2,true);
                                            SmartPutArvore(mensagem_split[1],true);
                                            // fazer um smart put nas lista de arvores ativas
                                        }finally {l_ativos.unlock();}

                                        Thread t1 = new Thread(() -> {
                                            try {
                                                servidor_stream(ip_a_enviar2);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        try {
                                            l_thread.lock();
                                            SmartPut(ip_a_enviar2,t1);
                                        }finally {l_thread.unlock();}
                                        t1.start();

                                        sendSream(ip_a_enviar2,mensagem_split[1]);

                                        Thread t = new Thread( () -> AddArvore(mensagem_split[1],ip_a_enviar2) );
                                        t.start();

                                        System.out.println("Eu "+ this.ip + "estou pronto para stremar!!!");
                                        break;
                                    case "Acabou":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore  a desativar
                                        boolean b;
                                        try {
                                            l_ativos.lock();
                                            l_arvores_ativas.lock();
                                            SmartPut(ip,false);
                                            SmartPutArvore(mensagem_split[1],false);

                                            b = this.lista_estados.values().stream().allMatch(value -> value.equals(false));
                                            // fazer um smartPut nas arvores ativas

                                        }finally {l_ativos.unlock(); l_arvores_ativas.unlock();}

                                        String ip_a_enviar = QuemEnviarTopDown(mensagem_split[1]);
                                        Thread temp;
                                        try {
                                            l_thread.lock();
                                            temp = lista_threads.get(ip);
                                        }finally {l_thread.unlock();}

                                        temp.interrupt();

                                        if (b) {
                                            sendAcabou(ip_a_enviar,mensagem_split[1]);
                                            System.out.println("Eu " + this.ip + " interrompi a stream do " + ip);
                                            this.Stremar = false;
                                        }
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
                                        System.out.println(mensagem_split[1]);
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
                                }
                            }
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

        }).start();
    }

    private void servidor_stream(String ip_vizinho) throws IOException {
        ServerSocket ouvinte_mestre = new ServerSocket(this.porta_strems);
        Socket ouvinte = ouvinte_mestre.accept();
        BufferedReader leitor_vizinho = new BufferedReader(new InputStreamReader(ouvinte.getInputStream()));

        Socket streamSocket = new Socket("localhost", this.vizinhos_udp.get(ip_vizinho));
        PrintWriter escritor = new PrintWriter(streamSocket.getOutputStream(), true);

        while (true) {
            String mensagem = leitor_vizinho.readLine();
            escritor.println(mensagem);
        }

    }

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


    private  void  escritor_vizinho(int porta_do_Node_a_enviar, String mensagem)throws IOException{

        Socket vizinho_a_enviar;
        PrintWriter escritor;

        vizinho_a_enviar = new Socket("localhost", porta_do_Node_a_enviar);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        escritor.println(mensagem);

        //System.out.println("Enviei estes vizinhos: " + arvores_incompletas + "para este Node " + porta_do_Node_a_enviar);
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

        System.out.println(this.vizinhos.get(ip_do_vizinho_a_enviar));
        porta_vizinho = this.vizinhos.get(ip_do_vizinho_a_enviar);

        vizinho_a_enviar = new Socket("localhost", porta_vizinho);
        escritor = new PrintWriter(vizinho_a_enviar.getOutputStream(), true);

        escritor.println(mensagem);

        //System.out.println("Enviei estes vizinhos: " + arvores_incompletas + "para este Node " + ip_do_vizinho_a_enviar);
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
            if (!b){String temp[] = {ip,arvore_nova}; Integer d = this.arvores_completas.size()+1; this.arvores_completas.put(d,temp);}
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
            latencia.put(ip_do_vizinho_a_enviar, tempo_ini);
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
