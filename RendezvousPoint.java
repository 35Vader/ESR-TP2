import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RendezvousPoint extends Node {

    private final List<Node> nosIntermedios;

    public RendezvousPoint(String ip, int porta, int portaBootstraper) throws IOException {
        super(ip, porta, portaBootstraper);
        this.nosIntermedios = new ArrayList<>();
    }

    // Iniciar o RendezvousPoint numa thread separada
    public void iniciar() {
        new Thread(() -> {
            try (ServerSocket servidorSocket = new ServerSocket(porta)) {
                System.out.println("RP iniciado na porta " + porta);

                // Aguarda por conexões de clientes
                while (true) {
                    Socket clienteSocket = servidorSocket.accept();
                    System.out.println("Conexão estabelecida com um cliente.");

                    // Tratamento do conteúdo recebido numa nova thread
                    new Thread(() -> processarConteudo(clienteSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Processa o conteúdo recebido de um nó
    private void processarConteudo(Socket clienteSocket) {
        try {
            // Recebe o conteúdo do cliente
            String conteudo = receberConteudo(clienteSocket);
            // Divide o conteúdo em partes usando "-"
            String[] partes = conteudo.split("-");
            // Verifica se o formato da mensagem é válido
            if (partes.length == 2) {
                String ip = partes[0];
                String mensagem = partes[1];
                System.out.println("Conteúdo recebido no RP de " + ip + ": " + mensagem);
                // Distribui o conteúdo pelos nós
                distribuirConteudo(ip, mensagem);
            } else {
                System.out.println("Formato de mensagem inválido: " + conteudo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Recebe o conteúdo do cliente
    private String receberConteudo(Socket clienteSocket) throws IOException {
        try (Scanner leitor = new Scanner(clienteSocket.getInputStream())) {
            return leitor.nextLine();
        }
    }

    // Distribui o conteúdo por todos os nós
    private void distribuirConteudo(String ipOrigem, String mensagem) {
        for (Node no : nosIntermedios) {
            // Adapta enviarConteudo para incluir o IP de origem
            no.enviarConteudo(ipOrigem, mensagem);
        }
    }

    // Adiciona um nó à lista
    public void adicionarNoIntermediario(Node no) {
        nosIntermedios.add(no);
    }
}
