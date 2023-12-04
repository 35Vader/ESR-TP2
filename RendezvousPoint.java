import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

public class RendezvousPoint{
    private final ReentrantLock l_latencia = new ReentrantLock();
    private final AddressingTable addressingTable;
    private AtomicInteger contadorMensagensRecebidas = new AtomicInteger(0);
    private final Timer timerAtualizacaoArvore = new Timer();
    private final ReentrantLock l_arvores = new ReentrantLock();
    private final HashMap<String, Long> latenciaClientes = new HashMap<>();
    private final HashMap<String, List<String>> arvoresClientes = new HashMap<>();
    private final static HashMap<String, String> caminhoClientes = new HashMap<>();
    private final Lock l_filaEspera = new ReentrantLock();
    private final Map<String, List<String>> filaDeEspera = new HashMap<>();
    
    // flag se diz se eu estou a stremar ou não
    private boolean Stremar = false;

    // ip do Node
    private final String ip;

    // porta do socket de tcp
    protected final int porta;

    // porta do socket udp para enviar strems
    private final int porta_strems;

    // ip->porta_tcp
    private final HashMap<String, Integer> vizinhos = new HashMap<>();

    //lock das listas de latencias
    private final ReentrantLock l_lantencia = new ReentrantLock();

    // lock da lista de mensagem de caminhos
    private final ReentrantLock l_mensagem = new ReentrantLock();

    // lock da lista de vizinhos
    private final ReentrantLock l_vizinhos = new ReentrantLock();

    // lock da lista de vizinhos
    private final ReentrantLock l_vizinhos_udp = new ReentrantLock();

    // lock da lista de vizinhos
    private final ReentrantLock l_arvores_completas = new ReentrantLock();

    //ip->porta_udp
    private final HashMap<String, Integer> vizinhos_udp = new HashMap<>();

    // lock da lista dos estados dos vizinhos
    private final ReentrantLock l_ok = new ReentrantLock();

    // ip -> "ok" ou ""
    private final HashMap<String, String> estados_de_vizinhos = new HashMap<>();

    //ip do que me enviou Arvore? -> arvore incompleta
    private final HashMap<String, String> mensagem = new HashMap<>();

    // ip vizinhos que quero medir as latencia -> tempo que mandei a mensaguem
    private final HashMap<String, Long> latencia = new HashMap<>();

    // id da arvore-> [ip_de_quem-eu_quero_enviar em BottomUp, arvore_completa]
    private final HashMap<Integer, String[]> arvores_completas = new HashMap<>();


    public RendezvousPoint(String ip, int porta, int porta_bootstraper, int porta_strems, Map<String, String> vizinhos)
            throws IOException {
        super();
        iniciarTimerAtualizacaoArvore();
        this.ip = ip;
        this.porta_strems = porta_strems;
        this.porta = porta;
        addressingTable = new AddressingTable(vizinhos);
        servidor();
        receberStreams();
    }

    private void mensagemRecebida() {
        AtomicInteger contadorMensagensRecebidas = new AtomicInteger(0);
        contadorMensagensRecebidas.incrementAndGet();
    }

    private void receberMensagemDoCliente(String clienteIP, String mensagem) {
        // Lógica para processar mensagem recebida do cliente
        System.out.println("Recebendo mensagem do cliente " + clienteIP + ": " + mensagem);
    
        // Adicione a mensagem à fila de espera
        adicionarMensagemFilaEspera(clienteIP, mensagem);
    }
    
    // Método para adicionar a mensagem à fila de espera
    private void adicionarMensagemFilaEspera(String clienteIP, String mensagem) {
        try {
            // Bloqueia o acesso à fila de espera para evitar conflitos
            l_filaEspera.lock();
    
            // Verifica se o cliente já está na fila
            if (filaDeEspera.containsKey(clienteIP)) {
                // Adiciona a mensagem à lista existente
                filaDeEspera.get(clienteIP).add(mensagem);
            } else {
                // Cria uma nova lista de mensagens para o cliente
                List<String> novaLista = new ArrayList<>();
                novaLista.add(mensagem);
                filaDeEspera.put(clienteIP, novaLista);
            }
        } finally {
            // Libera o bloqueio após concluir as operações na fila de espera
            l_filaEspera.unlock();
        }
    }
       

    // Este método cria uma thread para receber streams num loop infinito.
    // Os dados recebidos são processados diretamente neste método.
    // Se todas as mensagens forem recebidas, o método escolherCaminhos é chamado.
    private void receberStreams() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(getPortaStrems());
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

                    // Lógica de processamento da stream
                    processarStream(data, receivePacket.getAddress().getHostAddress());

                    // Exemplo: imprimir mensagem para demonstração
                    System.out.println("RendezvousPoint recebendo streams...");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private SocketAddress getPortaStrems() {
        return null;
    }

    // Método principal para processar a stream
    private void processarStream(byte[] data, String clienteIP) {
        try {
            String mensagem = new String(data);

            // Extrai o tipo e a mensagem da string recebida
            String[] partes = mensagem.split("/");
            if (partes.length == 2) {
                String tipo = partes[0];
                String mensagemConteudo = partes[1];

                // Lógica para processar a mensagem com base no tipo
                if ("bootup".equals(tipo)) {
                    // Envia mensagens bottom-up
                    addressingTable.turnOn(clienteIP);
                } else if ("topdown".equals(tipo)) {
                    // Recebe mensagens top-down
                    addressingTable.turnOff(clienteIP);
                    receberMensagemDoCliente(clienteIP, mensagemConteudo);
                }

                // Atualize as informações de latência para o cliente
                atualizarLatencia(clienteIP, 0); 

                // Chama o método para indicar que uma mensagem foi recebida
                mensagemRecebida();

                // Verifique se todas as mensagens foram recebidas
                if (todasMensagensRecebidas()) {
                    escolherCaminhos();
                }

                System.out.println("Processando stream: " + mensagem);
            } else {
                System.out.println("Mensagem inválida: " + mensagem);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    
    private void atualizarLatencia(String clienteIP, int novaLatencia) {
        try {
            l_latencia.lock();
    
            if (latenciaClientes.containsKey(clienteIP)) {
                Long latenciaAntiga = latenciaClientes.get(clienteIP);
    
                if (novaLatencia > latenciaAntiga) {
                    // A nova latência é maior
                    System.out.println("A nova latência é maior para " + clienteIP);
                    ajustarCaminho(clienteIP);
                } else if (novaLatencia < latenciaAntiga) {
                    // A nova latência é menor
                    System.out.println("A nova latência é menor para " + clienteIP);
                    latenciaClientes.put(clienteIP, (long) novaLatencia);
                    atualizarCaminho(clienteIP);
                } else {
                    // A nova latência é igual à latência existente
                    System.out.println("A nova latência é igual para " + clienteIP);
                }
            } else {
                // Adicionar nova latência
                latenciaClientes.put(clienteIP, (long) novaLatencia);
            }
    
        } finally {
            l_latencia.unlock();
        }
    }
    

    // Atualiza o caminho quando a latência é menor.
    private void atualizarCaminho(String clienteIP) {
        try {
            l_latencia.lock();

            long menorLatencia = Long.MAX_VALUE;
            String caminhoMenorLatencia = null;

            // Itera sobre todas as latências dos clientes
            for (Map.Entry<String, Long> entry : latenciaClientes.entrySet()) {
                String ipVizinho = entry.getKey();
                long latencia = entry.getValue();

                // Latência do cliente atual é menor que a menor latência já registada
                if (latencia < menorLatencia) {
                    menorLatencia = latencia;
                    caminhoMenorLatencia = ipVizinho;
                }
            }

            if (caminhoMenorLatencia != null && caminhoMenorLatencia.equals(clienteIP)) {
                System.out.println("Atualizando caminho para " + clienteIP + ". Menor latência para " + caminhoMenorLatencia);
            } else {
                System.out.println("Nenhum caminho disponível ou não é necessário atualizar para " + clienteIP);
            }

        } finally {
            l_latencia.unlock();
        }
    }

    // Iniciar o timer de atualização da árvore
    private void iniciarTimerAtualizacaoArvore() {
        // Agende a tarefa para ser executada a cada 10 segundos
        timerAtualizacaoArvore.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Lógica para atualização da árvore
                System.out.println("Atualizando árvore...");
            }
        }, 0, 10000); // 0 indica o atraso inicial, 10000 indica o intervalo em milissegundos (10 segundos)
    }

    // Adiciona uma árvore à lista de árvores de um cliente
    public void adicionarArvoreCliente(String clienteIP, String arvore) {
        try {
            l_arvores.lock();

            // Verifica se o cliente já possui uma lista de árvores
            if (arvoresClientes.containsKey(clienteIP)) {
                // Adiciona a nova árvore à lista existente
                List<String> listaArvores = arvoresClientes.get(clienteIP);
                listaArvores.add(arvore);
            } else {
                // Cria uma nova lista de árvores para o cliente
                List<String> novaLista = new ArrayList<>();
                novaLista.add(arvore);
                arvoresClientes.put(clienteIP, novaLista);
            }

        } finally {
            l_arvores.unlock();
        }
    }

    // Obtém a lista de árvores de um cliente específico
    public List<String> obterArvoresCliente(String clienteIP) {
        try {
            l_arvores.lock();

            return arvoresClientes.getOrDefault(clienteIP, Collections.emptyList());

        } finally {
            l_arvores.unlock();
        }
    }

    
    // Ajusta o caminho com base na latência, escolhendo o caminho com menor latência.
    private void ajustarCaminho(String clienteIP) {
        try {
            l_latencia.lock();
    
            long menorLatencia = Long.MAX_VALUE;
            String caminhoMenorLatencia = null;
    
            // Itera sobre todas as latências dos clientes
            for (Map.Entry<String, Long> entry : latenciaClientes.entrySet()) {
                String ipVizinho = entry.getKey();
                long latencia = entry.getValue();
    
                if (latencia < menorLatencia) {
                    menorLatencia = latencia;
                    caminhoMenorLatencia = ipVizinho;
                }
            }
    
            if (caminhoMenorLatencia != null) {
                // Caminho com menor latência encontrado
                System.out.println("Caminho ajustado para " + clienteIP + ". Menor latência para " + caminhoMenorLatencia);
            } else {
                // Não há caminho disponível 
                System.out.println("Nenhum caminho disponível para " + clienteIP);
            }
    
        } finally {
            l_latencia.unlock();
        }
    }
    

    // Cria uma HashMap<String, String> no formato 'IP_Cliente;IP_Node1,Latência1-2,IP_Node2!IP_Node2,Latência2-3,IP_Node3!IP_Node3,Latência3-4,IP_Node4...' 
    // onde sabe que se separar a HashMap por ';'' obtém no primeiro argumento ([0]) o IP_Cliente, se separar por '!' separa o segundo argumento ([1])
    // (caminho total da árvore) em caminhos separados por nodes, incluindo a latência e se separar por ',' separa os IPs dos nodes (argumento 1 e 3)
    // e a latência no meio

    public static HashMap<String, String> criarCaminhos(String input) {

        // Divide a entrada pelo caractere ';'
        String[] partes = input.split(";");
        if (partes.length == 2) {
            // Argumento 0: IP_Cliente
            caminhoClientes.put("IP_Cliente", partes[0]);

            // Argumento 1: Caminho total da árvore
            String caminhoTotal = partes[1];
            String[] caminhosSeparados = caminhoTotal.split("!");

            // Constrói a representação dos caminhos na HashMap
            StringBuilder caminhosMap = new StringBuilder();
            for (String caminho : caminhosSeparados) {
                String[] detalhesCaminho = caminho.split(",");
                if (detalhesCaminho.length == 3) {
                    // Detalhes do caminho: IP_Node, Latência, IP_Node
                    String ipNodeOrigem = detalhesCaminho[0];
                    String latencia = detalhesCaminho[1];
                    String ipNodeDestino = detalhesCaminho[2];

                    // Adiciona ao StringBuilder
                    caminhosMap.append(ipNodeOrigem)
                            .append(",")
                            .append(latencia)
                            .append(",")
                            .append(ipNodeDestino)
                            .append(",");
                }
            }

            // Adiciona os caminhos à HashMap
            caminhoClientes.put("Caminhos", caminhosMap.toString());
        }

        return caminhoClientes;
    }

    // Verifica se todas as mensagens esperadas foram recebidas.
    private boolean todasMensagensRecebidas() {
        int totalMensagensEsperadas = 10; // Número de mensagens?

        return contadorMensagensRecebidas.get() >= totalMensagensEsperadas;
    }

    // FALTA ACABAR
    // Adicione a lógica para escolher o caminho com base na latência
    private void escolherCaminho(String clienteIP, long latencia) {
        final long LIMITE_LATENCIA = 100;   

        if (latencia < LIMITE_LATENCIA) {
            // Se a latência for menor que o limite
            System.out.println("Escolher caminho rápido para " + clienteIP);
            // Lógica específica para caminhos rápidos
        } else {
            // Se a latência for maior ou igual ao limite
            System.out.println("Escolher caminho padrão para " + clienteIP);
            // Lógica específica para caminhos padrão
        }
    }

    // Método para encontrar o cliente com a menor latência
    private String encontrarClienteComMenorLatencia() {
        long menorLatencia = Long.MAX_VALUE;
        String clienteComMenorLatencia = null;

        for (Map.Entry<String, Long> entry : latenciaClientes.entrySet()) {
            String clienteIP = entry.getKey();
            long latencia = entry.getValue();

            if (latencia < menorLatencia) {
                menorLatencia = latencia;
                clienteComMenorLatencia = clienteIP;
            }
        }

        return clienteComMenorLatencia;
    }


    private void escolherCaminhos() {
        try {
            l_latencia.lock();

            // Verifique se há clientes na tabela de latência
            if (latenciaClientes.isEmpty()) {
                System.out.println("Sem clientes na tabela de latência.");
                return;
            }

            // Encontrar o cliente com a menor latência
            String clienteComMenorLatencia = encontrarClienteComMenorLatencia();
        
           // Verifique se o cliente com menor latência foi encontrado
            if (clienteComMenorLatencia != null) {
                escolherCaminho(clienteComMenorLatencia, latenciaClientes.get(clienteComMenorLatencia));
            } else {
                System.out.println("Não foi possível encontrar o cliente com a menor latência.");
            }
        } finally {
            l_latencia.unlock();
        }
    }


    // Cria uma nova thread para aguardar e processar a lista de árvores, incluindo a chamada para escolherCaminhos após o processamento.
    public void aguardarArvores() {
        new Thread(() -> {
            try {
                // Aguardar a lista de árvores
                Thread.sleep(5000); // Aguarda por 5 segundos
    
                // FALTA ACABAR
                // Processar a lista de árvores
                System.out.println("RendezvousPoint processando lista de árvores...");
    
                // Comparar latências e escolher caminhos
                escolherCaminhos();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    // Compara latência para cada cliente, chamando ajustarCaminho para cada cliente
    public void compararLatencia() {
        try {
            // Lógica para comparar latência e escolher caminho para cada cliente
            
            for (Map.Entry<String, Long> entry : latenciaClientes.entrySet()) {
                String clienteIP = entry.getKey();
                long latencia = entry.getValue();
    
                System.out.println("Comparando latência para o cliente " + clienteIP + ": " + latencia);
    
                ajustarCaminho(clienteIP); 
            }    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void servidor() {
        // Inicia uma nova thread para o servidor
        new Thread(() -> {
            try (ServerSocket ouvinte_mestre = new ServerSocket(this.porta)) {
                // Abre um socket do servidor na porta especificada
                
                // Thread para leitura de mensagens de todos os clientes
                new Thread(() -> {
                    try {
                        // Loop infinito para aguardar a conexão de clientes
                        while (true) {
                            // Aguarda a conexão de um cliente
                            Socket clienteSocket = ouvinte_mestre.accept();
                            
                            // Cria um leitor para receber mensagens do cliente
                            BufferedReader leitor_cliente = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                            String mensagem;
                            
                            // Loop para processar mensagens recebidas dos clientes
                            while ((mensagem = leitor_cliente.readLine()) != null) {
                                // Lógica para processar mensagens recebidas dos clientes
                                processarMensagemCliente(mensagem);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
    
                // Thread para enviar mensagens para os clientes
                new Thread(() -> {
                    // Loop infinito para enviar mensagens aos clientes
                    while (true) {
                        // Verifica se há mensagens na fila de espera
                        if (!getFilaDeEspera().values().isEmpty()) {
                            String mensagem;
                            String clienteIP;
   
                            try {
                                // Bloqueia o acesso à fila de espera para evitar conflitos
                                ((Lock) getFilaDeEspera()).lock();
                                clienteIP = ChooseKey(getFilaDeEspera());
                                mensagem = getFilaDeEspera().get(clienteIP).get(0);
                                getFilaDeEspera().get(clienteIP).remove(0);
                            } finally {
                                // Liberta o bloqueio após concluir as operações na fila de espera
                                ((Lock) getFilaDeEspera()).unlock();
                            }
   
                            // [tipo, mensagem]
                            String[] mensagem_split = mensagem.split("/");
   
                            // Lógica para enviar mensagens para os clientes com base no tipo
                            switch (mensagem_split[0]) {
                                case "stream":
                                    // Lógica para enviar stream para o cliente
                                    enviarStreamParaCliente(clienteIP, mensagem_split[1]);
                                    break;
                                default:
                                    System.out.println("Tipo de mensagem inválido");
                            }
                        }
                    }
                }).start();
    
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private String ChooseKey(HashMap<String, ArrayList<String>> filaDeEspera) {
        return null;
    }

    private HashMap<String, ArrayList<String>> getFilaDeEspera() {
        return null;
    }

    // Método para processar mensagens recebidas dos clientes
    private void processarMensagemCliente(String mensagem) {
        String[] mensagem_split = mensagem.split("-");
        String ip = mensagem_split[0];
        String tipo = mensagem_split[1];
        Thread t1 = null;
    
        try {
            switch (tipo) {
                case "ok?":
                    // Diz o estado ao vizinho que enviou a mensagem
                    escritor_vizinho(ip, this.ip + "-ok/");
                    break;
    
                case "ok":
                    // Atualiza o estado do vizinho como "ok"
                    try {
                        l_ok.lock();
                        this.estados_de_vizinhos.put(ip, "ok");
                    } finally {
                        l_ok.unlock();
                    }
                    break;
    
                case "Vizinhos":
                    // Atualiza a lista de vizinhos
                    try {
                        l_vizinhos.lock();
                        l_vizinhos_udp.lock();
                        SetVizinhos(mensagem_split[1]);
                    } finally {
                        l_vizinhos.unlock();
                        l_vizinhos_udp.unlock();
                    }
                    // Segunda fase
                    okVizinhos();
                    break;
    
                case "metricas?":
                    // Envia uma mensagem métrica para o vizinho que pediu
                    escritor_vizinho(ip, this.ip + "-metrica/" + mensagem_split[1]);
                    break;
    
                case "metrica":
                    // Para o timer para o IP, envia mensagem Arvore para este vizinho com métricas atualizadas
                    long tempo_fim = System.currentTimeMillis(); // Medição da latência em roundtrip
                    long latencia;
    
                    try {
                        l_lantencia.lock();
                        latencia = tempo_fim - this.latencia.get(ip);
                    } finally {
                        l_lantencia.unlock();
                    }
    
                    // Métricas atualizadas
                    try {
                        l_mensagem.lock();
                        String arvore = this.mensagem.get(mensagem_split[1]);
                        String arvore_atualizada = arvore + "!" + this.ip + latencia + ip;
                        // Propaga o fluxo para a construção de árvores de forma top-down
                        escritor_vizinho(ip, this.ip + "-Arvore?/" + arvore_atualizada);
                    } finally {
                        l_mensagem.unlock();
                    }
                    break;
    
                case "Arvore?":
                    // Atualiza a árvore recebida
                    try {
                        l_mensagem.lock();
                        this.mensagem.put(ip, mensagem_split[1]);
                    } finally {
                        l_mensagem.unlock();
                    }
                    metricas(ip);
                    break;
    
                case "Arvore":
                    // Adiciona uma nova árvore à lista de árvores completas
                    String ip_enviar3;
    
                    try {
                        // RP tem este IP 121.191.51.101
                        // "121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> árvore escolhida pelo RP
                        l_arvores_completas.lock();
                        ip_enviar3 = QuemEnviarBottomUp(mensagem_split[1]);
                        int key = this.arvores_completas.size() + 1;
                        String[] temp = { ip_enviar3, mensagem_split[1] };
                        this.arvores_completas.put(key, temp);
                    } finally {
                        l_arvores_completas.unlock();
                    }
                    sendArvoreAtiva(ip_enviar3, mensagem_split[1]);
                    break;

                    case "Stream?":
                    if (!Stremar) {
                        try {
                            l_arvores_completas.lock();
                            if (this.arvores_completas.isEmpty()) {
                                try {
                                    l_mensagem.lock();
                                    this.mensagem.put(ip, mensagem_split[2]);
                                } finally {
                                    l_mensagem.unlock();
                                }
                                metricas(ip);
                            } else {
                                if (this.arvores_completas.size() == 1) {
                                    sendTree(this.arvores_completas.get(1)[0]);
                                }
                                if (this.arvores_completas.size() > 1) {
                                    String ip_enviar = ChooseTree();
                                    sendTree(ip_enviar);
                                }
                            }
                        } finally {
                            l_arvores_completas.unlock();
                        }
                    } else {
                        t1 = new Thread(() -> servidor_stream(ip));
                        t1.start();
                    }
                    break;
                
                case "Stream":
                    //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> árvore ativa
                    this.Stremar = true;
                    String ip_a_enviar2 = QuemEnviarBottomUp(mensagem_split[2]);
                    t1 = new Thread(() -> servidor_stream(ip_a_enviar2));
                    t1.start();
                    sendSream(ip_a_enviar2, mensagem_split[3]);
                    break;
                
                case "Acabou":
                    //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> árvore a desativar
                    this.Stremar = false;
                    String ip_a_enviar = QuemEnviarTopDown(mensagem_split[2]);
                    t1.interrupt();
                    sendAcabou(ip_a_enviar, mensagem_split[3]);
                    break;
                
                case "Atualiza?":
                    //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> árvore ativa atualizada até mim
                    String ip_a_enviar1 = QuemEnviarBottomUp(mensagem_split[2]);
                    requestLatencia(ip_a_enviar1, mensagem_split[3]);
                    break;
                
                case "Atualisa":
                    escritor_vizinho(ip, this.ip + "-Atualizei/" + mensagem_split[2]);
                    break;
                
                case "Atualizei":
                    //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> árvore ativa atualizada até mim inclusive
                    long tempo_fim1 = System.currentTimeMillis();
                    long latencia1;
                    try {
                        l_lantencia.lock();
                        latencia1 = tempo_fim1 - this.latencia.get(ip);
                    } finally {
                        l_lantencia.unlock();
                    }
                    String arvore_atualizada1 = Atualiza(latencia1, mensagem_split[2]);
                    escritor_vizinho(ip, this.ip + "-Atualiza?/" + arvore_atualizada1);
                    break;
                
                case "ArvoreAtualizada":
                    //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> árvore ativa atualizada totalmente
                    try {
                        // Rp tem este ip 121.191.51.101
                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc"
                        l_arvores_completas.lock();
                        AtualizaArvores(ip, mensagem_split[2]);
                    } finally {
                        l_arvores_completas.unlock();
                    }
                    String ip_enviar4 = QuemEnviarTopDown(mensagem_split[3]);
                    sendArvoreAtualisada(ip_enviar4, mensagem_split[3]);
                    break;
                
        
                default:
                    System.out.println("Mensagem inválida");
            }
        } catch (Exception e) {
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

    private void AtualizaArvores(String ip_quem_me_enviou_top_down, String arvore_atualizada){
        for (Integer i: this.arvores_completas.keySet()){
            if(this.arvores_completas.get(i)[0].equals(ip_quem_me_enviou_top_down)){
                this.arvores_completas.get(i)[1] = arvore_atualizada;
            }

        }
    }

    private String Atualiza(Long novaLatencia, String arvore_a_atualizar){
        String[] caminhos = arvore_a_atualizar.split("!");
        for (String s : caminhos) {

            String[] partes = s.split(",");
            if (partes[0].equals(this.ip)) { partes[1] = novaLatencia.toString();break;}
        }

        return  joinArray(caminhos);
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

    public String QuemEnviarTopDown(String arvore) {
        String[] caminhos = arvore.split("!");
        String res = "";
        for (String s : caminhos) {

            String[] partes = s.split(",");
            if (partes[2].equals(this.ip)) { res = partes[0]; break; }
        }
        return res;
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
                    DatagramPacket sendPacket = new DatagramPacket(data, data.length, InetAddress.getByName(ip_vizinho), this.vizinhos_udp.get(ip_vizinho));
                    socket.send(sendPacket);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
}

    private String ChooseTree() {
        Long[] latencias_totais = new Long[this.arvores_completas.size()];

        for (Integer i : this.arvores_completas.keySet()) {
            latencias_totais[i - 1] = GetLatencia(this.arvores_completas.get(i)[1]);
        }
        return this.arvores_completas.get(mini(latencias_totais))[0];
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
    

    // Método para enviar stream para o cliente
    private void enviarStreamParaCliente(String clienteIP, String mensagem) {
        try {
            // Assume que você tem uma instância DatagramSocket para enviar dados
            DatagramSocket socket = new DatagramSocket();

            // Converte a mensagem para bytes
            byte[] mensagemBytes = mensagem.getBytes();
            int length = mensagemBytes.length;

            // Cria um ByteArrayOutputStream para construir os dados de envio
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteStream);

            // Escreve o comprimento e os dados no DataOutputStream
            dataOutputStream.writeInt(length);
            dataOutputStream.write(mensagemBytes);

            // Obtém os bytes finais a serem enviados
            byte[] data = byteStream.toByteArray();

            // Envia os dados para o cliente
            DatagramPacket packet = new DatagramPacket(data, data.length, getPortaStrems());
            socket.send(packet);

            // Fecha o socket após o envio
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

}
