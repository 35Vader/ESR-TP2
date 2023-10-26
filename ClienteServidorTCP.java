import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ClienteServidorTCP {

    public static <String> void main(String[] args) {
        final int porta = 12345;

        // Inicialização do servidor
        new Thread(() -> {
            try {
                try (ServerSocket servidorSocket = new ServerSocket(porta)) {
                    System.out.println("Servidor iniciado na porta " + porta);

                    while (true) {
                        Socket clienteSocket = servidorSocket.accept();
                        System.out.println("Conexão foi estabelecida com um cliente.");

                        // Configuração do leitor e escritor para o cliente
                        BufferedReader leitor = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                        PrintWriter escritor = new PrintWriter(clienteSocket.getOutputStream(), true);

                        // Leitura da mensagem do cliente e envio de volta
                        String mensagem = (String) leitor.readLine();
                        System.out.println("Cliente diz: " + mensagem);
                        escritor.println("Servidor ecoa: " + mensagem);

                        // Fecho da conexão com o cliente
                        clienteSocket.close();
                        System.out.println("Conexão com o cliente foi fechada.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Inicialização do cliente
        new Thread(() -> {
            try {
                // Tempo de espera para o servidor iniciar
                Thread.sleep(1000);

                Socket clienteSocket = new Socket("localhost", porta);

                // Configuração do leitor e escrita para o cliente
                BufferedReader leitor = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                PrintWriter escritor = new PrintWriter(clienteSocket.getOutputStream(), true);

                // Envio de uma mensagem para o servidor
                escritor.println("Olá, servidor!");

                // Resposta do servidor
                String resposta = (String) leitor.readLine();
                System.out.println("Resposta do servidor: " + resposta);

                // Fecho da conexão com o servidor
                clienteSocket.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}





