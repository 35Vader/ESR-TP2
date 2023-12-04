import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;


//mensagens para nós = ip-type/meng

public class Servidor {
    private String ip;
    private int porta;

    // ip -> [StreamIP; StreamEmSi]
    private ReentrantLock l_pedidos = new ReentrantLock();
    private HashMap<String, String[] > pedidos;

    ///lock da fila de espera;
    private final ReentrantLock l_fila_de_espera = new ReentrantLock();

    // ip -> [men]
    private final HashMap<String, ArrayList<String>> fila_de_espera = new HashMap<>();

    private void SmartPut(String ip, String mensagem, HashMap<String, ArrayList<String>> fila) {
        ArrayList<String> temp;
        if ((temp = fila.get(ip)) != null) temp.add(mensagem);

        else {
            temp = new ArrayList<>();
            temp.add(mensagem);
            fila.put(ip, temp);
        }

    }

    private void stream_sender(){
        new Thread(() -> {
            try (ServerSocket ouvinte_mestre = new ServerSocket(this.porta)) {
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

                new Thread( () -> {



                }).start();
            }
            catch (IOException e) { e.printStackTrace();}
        });
   }

}
