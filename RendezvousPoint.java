import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class RendezvousPoint extends Node {

    private final ReentrantLock l_latencia = new ReentrantLock();
    private final HashMap<String, Long> latenciaClientes = new HashMap<>();
    private AtomicInteger contadorMensagensRecebidas = new AtomicInteger(0);

    public RendezvousPoint(String ip, int porta, int porta_bootstraper, int porta_strems) throws IOException {
        super(ip, porta, porta_bootstraper, porta_strems);
    }

    private void mensagemRecebida() {
        contadorMensagensRecebidas.incrementAndGet();
    }

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
                    processarStream(data);

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

    private void processarStream(byte[] data) {
        try {
            String mensagem = new String(data);
    
            // Analise a mensagem para obter informações relevantes
            String[] partes = mensagem.split(",");
            String clienteIP = partes[0];
            long latencia = Long.parseLong(partes[1]);
    
            // Atualize as informações de latência para o cliente
            atualizarLatencia(clienteIP, latencia);
    
            // Chama o método para indicar que uma mensagem foi recebida
            mensagemRecebida();
    
            // Verifique se todas as mensagens foram recebidas
            if (todasMensagensRecebidas()) {
                escolherCaminhos();
            }
    
            System.out.println("Processando stream: " + mensagem);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

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
    
    
    private void atualizarCaminho(String clienteIP) {
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

            if (caminhoMenorLatencia != null && caminhoMenorLatencia.equals(clienteIP)) {
                System.out.println("Atualizando caminho para " + clienteIP + ". Menor latência para " + caminhoMenorLatencia);
                // Falta acabar
            } else {
                System.out.println("Nenhum caminho disponível ou não é necessário atualizar para " + clienteIP);
            }

        } finally {
            l_latencia.unlock();
        }
    }

    private boolean todasMensagensRecebidas() {
        int totalMensagensEsperadas = 10; // Número de mensagens?

        return contadorMensagensRecebidas.get() >= totalMensagensEsperadas;
    }

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
    

}
