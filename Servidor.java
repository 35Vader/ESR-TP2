import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Servidor {
    private String ip;
    private int porta;

    // ip -> [StreamIP; StreamEmSi]
    private ReentrantLock l_pedidos = new ReentrantLock();
    private HashMap<String, String[] > pedidos;

    private void stream_sender(){
        new Thread(() -> {
            try (ServerSocket ouvinte_mestre = new ServerSocket(this.porta)) {
                new Thread(() -> {
                    try {
                        Socket ouvinte = ouvinte_mestre.accept();
                        BufferedReader leitor_vizinho = new BufferedReader(new InputStreamReader(ouvinte.getInputStream()));
                        String mensagem;
                        while (true) {

                            //ip-tipo/mensg
                            if ( (mensagem = leitor_vizinho.readLine()) != null){

                                // [ip,tipo/mensg]
                                String [] ip_mensg = mensagem.split("-");

                                //[tipo, mensg]
                                String [] mensagem_split = ip_mensg[1].split("/");
                                try {
                                    l_pedidos.lock();
                                    this.pedidos.put(ip_mensg[0],mensagem_split);
                                } finally { l_pedidos.unlock();}

                            }
                        }
                    } catch (IOException e) {e.printStackTrace();}
                }).start();

                new Thread( () -> {



                }).start();
            }
            catch (IOException e) { e.printStackTrace();}
        });
   }

}
