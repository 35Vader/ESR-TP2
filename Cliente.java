import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Cliente {

    // ip do Cliente
    private final String ip;

    // porta do Nodo que está ligao a ele
    private final int porta_node_folha;

    // porta do socket udp para enviar strems
    private final int porta_strems;

    private final int porta;

    ///lock da fila de espera;
    private final ReentrantLock l_fila_de_espera = new ReentrantLock();

    // ip -> [men]
    private final HashMap<String, ArrayList<String>> fila_de_espera = new HashMap<>();

    public Cliente(String ip, int porta_node_folha, int porta_strems, int porta){
        this.ip = ip;
        this.porta_node_folha = porta_node_folha;
        this.porta_strems = porta_strems;
        this.porta = porta;
    }

    //recessor geral
    private void servidor() {
        // Uma especie de recessionista
        new Thread(() -> {
            try (ServerSocket ouvinte_mestre = new ServerSocket(this.porta)) {
                // ligação entre um vizinho e 'eu' (eu sou um Node)

                // Thread para leitura de mensagens de todos os seus vizinhos
                new Thread(() -> {
                    try {
                        Socket ouvinte = ouvinte_mestre.accept();
                        BufferedReader leitor_vizinho = new BufferedReader(new InputStreamReader(ouvinte.getInputStream()));
                        String mensagem;
                        while (true) {
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
                    try {
                        Socket escritor = ouvinte_mestre.accept();
                        PrintWriter escritor_vizinho = new PrintWriter(escritor.getOutputStream());
                        while (true) {
                            if (!this.fila_de_espera.values().isEmpty()) {
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

                                    case "Arvore":
                                        String ip_enviar3;
                                        try {
                                            // Rp tem este ip 121.191.51.101
                                            //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc"
                                            l_arvores_completas.lock();
                                            int key = this.arvores_completas.size() + 1;
                                            String temp = mensagem_split[1];
                                            this.arvores_completas.put(temp);
                                        } finally {
                                            l_arvores_completas.unlock();
                                        }
                                        sendArvore(ip_enviar3,mensagem_split[1]);
                                        break;

                                    case "Stream":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore ativa
                                        this.Stremar = true;
                                        String ip_a_enviar2 = QuemEnviarBottomUp(mensagem_split[1]);
                                        servidor_stream(ip_a_enviar2);
                                        sendSream(ip_a_enviar2,mensagem_split[1]);
                                        break;

                                    case "Atualiza?":
                                        //"121.191.51.101 ,10, 121.191.52.101!etc!etc!etc" -> arvore ativa atualizada totalmente
                                        String ip_a_enviar1 = QuemEnviarBottomUp(mensagem_split[1]);
                                        requestLatencia(ip_a_enviar1,mensagem_split[1]);
                                        break;

                                    case "Atualisa":
                                        escritor_vizinho.println(this.ip + "-Atualizei/" + mensagem_split[1]);
                                        break;


                                    default:
                                        System.out.println("Mensagem inválida");
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


}
