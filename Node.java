import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

public class Node {
    private String ip;
    private int porta;
    private Set<String> vizinhos = new HashSet<>();
    private String ip_bootstraper;
    private ConnectionManagerIdentifier connectionManagerIdentifier;

    private InetAddress group;
    private MulticastSocket multicastSocket;

    // Construtor
    public Node(String ip, int porta, ConnectionManagerIdentifier connectionManagerIdentifier, String ip_bootstraper, String multicastGroup, int multicastPort) throws IOException {
        this.ip = ip;
        this.porta = porta;
        this.ip_bootstraper = ip_bootstraper;
        this.connectionManagerIdentifier = connectionManagerIdentifier;

        // Configuração do socket multicast
        this.group = InetAddress.getByName(multicastGroup);
        this.multicastSocket = new MulticastSocket(multicastPort);
        this.multicastSocket.joinGroup(group);

        // Inicialização do Connection Manager
        connectionManagerIdentifier.setConnectionManagers(this.ip, this.porta);
    }

    private synchronized boolean isVizinho(String ip) {
        return vizinhos.contains(ip);
    }

    public void ler() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // Recebe dados do multicast
        multicastSocket.receive(packet);
        String mensagem = new String(packet.getData(), 0, packet.getLength());

        System.out.println("Mensagem recebida: " + mensagem);
    }
     
    private void processarMensagem(String mensagem) {
        // Suponha que a mensagem seja um evento específico, como "INICIO_STREAM"
        switch (mensagem) {
            case "INICIO_STREAM":
                // Lógica para lidar com o início de uma nova transmissão
                System.out.println("Iniciando nova transmissão...");
                break;
            case "FIM_STREAM":
                // Lógica para lidar com o término de uma transmissão
                System.out.println("Transmissão concluída.");
                break;
            // Adicione outros casos conforme necessário
            default:
                // Lógica padrão para mensagens desconhecidas
                System.out.println("Mensagem desconhecida: " + mensagem);
                break;
        }
    }

    public void escrever(String mensagem) throws IOException {
        // Envia dados para o grupo multicast
        multicastSocket.send(new DatagramPacket(mensagem.getBytes(), mensagem.length(), group, porta));
    }

    public void lerBootstraper() throws IOException {
        connectionManagerIdentifier.getConnectionManager(ip_bootstraper).read();
    }

    public void escreverBootstraper() throws IOException {
        connectionManagerIdentifier.getConnectionManager(ip_bootstraper).send("vizinhos", "vizinhos");
    }

    public String receberBootstraper() throws IOException, InterruptedException {
        String s = connectionManagerIdentifier.getConnectionManager(ip_bootstraper).receive("vizinhos");
        String[] ips = s.split(",");
        for (String ip : ips) {
            vizinhos.add(ip);
        }
        return s;
    }

    public synchronized void adicionarVizinho(String ip) {
        vizinhos.add(ip);
    }
    
    public synchronized void removerVizinho(String ip) {
        vizinhos.remove(ip);
    }
}
