import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.*;
import java.net.*;

public class UDPServer {
    public static void main(String[] args) {
        final int porta = 12345;

        // Thread para o servidor
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(porta)) {
                System.out.println("Servidor iniciado na porta " + porta);

                byte[] receiveData = new byte[1024];

                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);

                    // Obtém o endereço IP e a porta do cliente
                    InetAddress clientAddress = receivePacket.getAddress();
                    int clientPort = receivePacket.getPort();

                    // Converte os bytes recebidos para um DataInputStream
                    ByteArrayInputStream byteStream = new ByteArrayInputStream(receivePacket.getData());
                    DataInputStream dataInputStream = new DataInputStream(byteStream);

                    // Lê os dados do DataInputStream
                    int length = dataInputStream.readInt();
                    byte[] data = new byte[length];
                    dataInputStream.readFully(data);

                    // Processa os dados recebidos como bytes
                    System.out.println("Cliente enviou " + length + " bytes.");

                    // Envia uma resposta ao cliente
                    String resposta = "Servidor recebeu " + length + " bytes.";
                    byte[] sendData = resposta.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), clientPort);
                    socket.send(sendPacket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();


        // Thread para o cliente
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress serverAddress = InetAddress.getByName("localhost");

                // Dados a serem enviados como bytes
                byte[] data = "Hello, servidor!".getBytes();

                // Cria um DataOutputStream para facilitar a escrita de dados binários
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteStream);

                // Escreve o comprimento dos dados seguido pelos próprios dados
                dataOutputStream.writeInt(data.length);
                dataOutputStream.write(data);

                // Converte os dados para um array de bytes
                byte[] sendData = byteStream.toByteArray();

                // Envia os dados ao servidor
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,InetAddress.getByName("localhost") , porta);
                socket.send(sendPacket);

                // Recebe a resposta do servidor
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String resposta = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Resposta do servidor: " + resposta);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }}