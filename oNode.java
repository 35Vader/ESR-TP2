import java.io.*;
import java.net.*;
import java.util.Scanner;

public class oNode {
    public static void main(String[] args) {
        final int porta = 12345;

        // Inicie o servidor
        new Thread(() -> {
            try {
                try (ServerSocket servidorSocket = new ServerSocket(porta)) {
                    System.out.println("Servidor iniciado na porta " + porta);

                    while (true) {
                        Socket clienteSocket = servidorSocket.accept();
                        System.out.println("Conexão estabelecida com um cliente.");

                        // Configuração do leitor e do escritor para o cliente
                        BufferedReader leitor = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                        PrintWriter escritor = new PrintWriter(clienteSocket.getOutputStream(), true);

                        // Leitura e envio de mensagens do cliente
                        String mensagem;
                        while ((mensagem = leitor.readLine()) != null) {
                            System.out.println("Cliente diz: " + mensagem);

                            // Envio da resposta
                            escritor.println("Servidor ecoa: " + mensagem);
                        }

                        // Fecho da conexão com o cliente
                        clienteSocket.close();
                        System.out.println("Conexão com o cliente fechada.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Inicialização do cliente
        new Thread(() -> {
            try {
                Socket clienteSocket = new Socket("localhost", porta);

                // Configuração do leitor e do escritor para o cliente
                BufferedReader leitor = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                PrintWriter escritor = new PrintWriter(clienteSocket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in);

                // Envio e receção de mensagens do servidor
                System.out.print("intruduza mensagem: ");
                String s = scanner.nextLine();
                escritor.println(s);
                String resposta = leitor.readLine();
                System.out.println("Resposta do servidor: " + resposta);

                // Envio de outra mensagem
                System.out.print("intruduza mensagem: ");
                String s2 = scanner.nextLine();
                escritor.println(s2);
                resposta = leitor.readLine();
                System.out.println("Resposta do servidor: " + resposta);

                // Fecho da conexão com o servidor
                clienteSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
