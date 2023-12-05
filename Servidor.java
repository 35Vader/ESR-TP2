import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;


//mensagens para nós = ip-type/meng

public class Servidor {

    private int porta;
    private int porta_strems;
    private int porta_RP;

    ///lock da fila de espera;
    private final ReentrantLock l_fila_de_espera = new ReentrantLock();

    // loock da lista de threads
    private final ReentrantLock l_thread = new ReentrantLock();

    // ip -> Thread a interromper
    private final HashMap<String, Thread> lista_threads = new HashMap<>();

    // ip -> [men]
    private final HashMap<String, ArrayList<String>> fila_de_espera = new HashMap<>();


    public Servidor(int porta, int porta_strems, int porta_RP){
        this.porta = porta;
        this.porta_strems = porta_strems;
        this.porta_RP = porta_RP;
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

    public void inicializador(){
        servidor();
    }

    private void servidor() {
        // Uma especie de recessionista
        new Thread(() -> {


                // Thread para leitura de mensagens de todos os seus vizinhos
                new Thread(() -> {
                    try {
                        // ligação entre o RP  e 'eu' (eu sou um Servidor)
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

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                // uma especie de capataz
                new Thread(() -> {
                    while (true) {
                        if (!IsEmpty(this.fila_de_espera)) {
                            String mensagem;
                            String ip;
                            try {
                                l_fila_de_espera.lock();
                                ip = this.fila_de_espera.keySet().stream().toList().get(0);
                                mensagem = this.fila_de_espera.get(ip).get(0);
                                this.fila_de_espera.get(ip).remove(0);
                            } finally {
                                l_fila_de_espera.unlock();
                            }

                            // [tipo, meng]
                            String[] mensagem_split = mensagem.split("/");

                            switch (mensagem_split[0]) {

                                case "SendStream":
                                    System.out.println("Preparados ou não aqui vou eu stremar");
                                    Thread t1 = new Thread(() -> servidor_stream());
                                    try {
                                        l_thread.lock();
                                        lista_threads.put(ip,t1);
                                    }finally {l_thread.unlock();}

                                    try {
                                        Thread.sleep(300);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    t1.start();
                                    break;

                                case "Acabou":
                                    Thread t;
                                    System.out.println("Espero que têm gostado da stream :) ");
                                    try {
                                        l_thread.lock();
                                        t = lista_threads.get(ip);
                                    }finally {l_thread.unlock();}
                                    t.interrupt();
                                    break;

                                default:
                                    System.out.println("Mensagem inválida");
                            }
                        }
                    }
                }).start();

        }).start();
    }

    private void servidor_stream() {
        try (DatagramSocket socket = new DatagramSocket(this.porta_strems)) {
            try {
                while (true) {
                    // Ler dados do arquivo
                    ///String dados = lerDadosDoArquivo("stream.txt");

                    // Dados a serem enviados como bytes
                    byte[] data = "Olá".getBytes();

                    // Cria um DataOutputStream para facilitar a escrita de dados binários
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    DataOutputStream dataOutputStream = new DataOutputStream(byteStream);

                    // Escreve o comprimento dos dados seguido pelos próprios dados
                    dataOutputStream.writeInt(data.length);
                    dataOutputStream.write(data);

                    // Converte os dados para um array de bytes
                    byte[] sendData = byteStream.toByteArray();

                    // Envia os dados ao RP
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), porta_RP);
                    socket.send(sendPacket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private String lerDadosDoArquivo(String caminhoArquivo) throws IOException {
        StringBuilder conteudo = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                conteudo.append(linha).append("\n");
            }
        }
        return conteudo.toString();
    }



}
