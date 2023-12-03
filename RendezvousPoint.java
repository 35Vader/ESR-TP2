import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RendezvousPoint extends Node {

    // l_latencia: garantir a sincronização em operações relacionadas à latência.
    // latenciaClientes: armazena informações de latência para cada cliente identificado por seu endereço IP.
    // caminhoClientes: armazena informações de caminhos para cada cliente identificado por seu endereço IP.
    // contadorMensagensRecebidas: número de mensagens recebidas.
    private final ReentrantLock l_latencia = new ReentrantLock();
    private final HashMap<String, Long> latenciaClientes = new HashMap<>();
    private final static HashMap<String, String> caminhoClientes = new HashMap<>();
    private AtomicInteger contadorMensagensRecebidas = new AtomicInteger(0);
    private final Timer timerAtualizacaoArvore = new Timer();
    private final ReentrantLock l_arvores = new ReentrantLock();
    private final HashMap<String, List<String>> arvoresClientes = new HashMap<>();

    public RendezvousPoint(String ip, int porta, int porta_bootstraper, int porta_strems) throws IOException {
        super(ip, porta, porta_bootstraper, porta_strems);
        iniciarTimerAtualizacaoArvore();
        // Inicie o servidor ao criar uma instância de RendezvousPoint
        servidor();
    }

    private void mensagemRecebida() {
        contadorMensagensRecebidas.incrementAndGet();
    }

    private void enviarMensagemParaCliente(String clienteIP, String mensagem) {
        // Lógica para enviar mensagem para o cliente
        // Você pode usar sockets ou outro mecanismo de comunicação aqui
        System.out.println("Enviando mensagem para o cliente " + clienteIP + ": " + mensagem);
    }

    private void receberMensagemDoCliente(String clienteIP, String mensagem) {
        // Lógica para processar mensagem recebida do cliente
        System.out.println("Recebendo mensagem do cliente " + clienteIP + ": " + mensagem);
    }    

    // Este método cria uma thread para receber streams num loop infinito.
    // Os dados recebidos são processados diretamente neste método.
    // Se todas as mensagens forem recebidas, o método escolherCaminhos é chamado.
    public void receberStreams() {
        new Thread(() -> {
            try {
                // Lógica para receber e processar streams
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

    // Envia mensagem para um nó específico
    private void enviarMensagemParaNodo(String clienteIP, String mensagem) {
        // Lógica para enviar mensagem para o nó
        System.out.println("Enviando mensagem para o nó " + clienteIP + ": " + mensagem);
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
                    enviarMensagemParaNodo(clienteIP, "ip-type/meng");
                } else if ("topdown".equals(tipo)) {
                    // Recebe mensagens top-down
                    receberMensagemDoCliente(clienteIP, mensagemConteudo);
                }

                // Atualize as informações de latência para o cliente
                atualizarLatencia(clienteIP, 0);  // Aqui você pode ajustar a latência conforme necessário

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

    
    
    // Atualiza as informações de latência para um cliente específico.
    private void atualizarLatencia(String clienteIP, long latencia) {
        try {
            l_latencia.lock();
    
            if (latenciaClientes.containsKey(clienteIP)) {
                long latenciaAntiga = latenciaClientes.get(clienteIP);
    
                if (latencia > latenciaAntiga) {
                    // A nova latência é maior
                    System.out.println("A nova latência é maior para " + clienteIP);
                    ajustarCaminho(clienteIP);
                } else if (latencia < latenciaAntiga) {
                    // A nova latência é menor
                    System.out.println("A nova latência é menor para " + clienteIP);
                    latenciaClientes.put(clienteIP, latencia);
                    atualizarCaminho(clienteIP);
                } else {
                    // A nova latência é igual à latência existente
                    System.out.println("A nova latência é igual para " + clienteIP);
                }
            } else {
                // Adicionar nova latência
                latenciaClientes.put(clienteIP, latencia);
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

    // Compara latências e escolhe caminhos
    private void escolherCaminhos() {
        // Lógica para comparar latências e escolher caminhos
        try {
            l_latencia.lock();
            // Itera sobre os clientes para escolher o caminho com menos latência
            for (String clienteIP : latenciaClientes.keySet()) {
                long latencia = latenciaClientes.get(clienteIP);

                // Por acabar
                System.out.println("Escolhendo caminho para o cliente " + clienteIP + " com latência " + latencia);
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
    
            System.out.println("RendezvousPoint comparando latência...");
    
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


    private HashMap<String, ArrayList<String>> getFilaDeEspera() {
        return null;
    }

    // Método para processar mensagens recebidas dos clientes
    private void processarMensagemCliente(String mensagem) {
        // Lógica para processar mensagens recebidas dos clientes
        System.out.println("Recebendo mensagem do cliente: " + mensagem);
    }

    // Método para enviar stream para o cliente
    private void enviarStreamParaCliente(String clienteIP, String mensagem) {
        // Lógica para enviar stream para o cliente
        System.out.println("Enviando stream para o cliente " + clienteIP + ": " + mensagem);
    }
    

}
