import java.io.IOException;
import java.util.HashMap;

public class Main {
    private static void MakeBootstraper(Bootstraper bootstraper){

            HashMap<String,Integer> vizinhos_bootstraper_tcp = new HashMap<>();
            vizinhos_bootstraper_tcp.put("2.2.2.2",2222);
            vizinhos_bootstraper_tcp.put("6.6.6.6",6666);

            HashMap<String,Integer> vizinhos_bootstraper_udp = new HashMap<>();
            vizinhos_bootstraper_udp.put("2.2.2.2",2220);
            vizinhos_bootstraper_udp.put("6.6.6.6",6660);

            bootstraper.setvizinhos(vizinhos_bootstraper_tcp,vizinhos_bootstraper_tcp);

            HashMap<String,Integer> vizinhos_um_tcp = new HashMap<>();
            vizinhos_um_tcp.put("2.2.2.2",2222); // Node2
            vizinhos_um_tcp.put("6.6.6.6",6666); //RP

            HashMap<String,Integer> vizinhos_um_udp = new HashMap<>();
            vizinhos_um_udp.put("2.2.2.2",2220); // Node2
            vizinhos_um_udp.put("6.6.6.6",6660); //RP

            bootstraper.setTopologia("1.1.1.1",vizinhos_um_tcp,vizinhos_um_udp);

            HashMap<String,Integer> vizinhos_dois_tcp = new HashMap<>();
            vizinhos_dois_tcp.put("1.1.1.1",1111);// Node1
            vizinhos_dois_tcp.put("5.5.5.5",5555); // Bootstraper ou Node5
            vizinhos_dois_tcp.put("3.3.3.3",3333); // Node3

            HashMap<String,Integer> vizinhos_dois_udp = new HashMap<>();
            vizinhos_dois_udp.put("1.1.1.1",1110);// Node1
            vizinhos_dois_udp.put("5.5.5.5",5550); // Bootstraper ou Node5
            vizinhos_dois_udp.put("3.3.3.3",3330); // Node3


            bootstraper.setTopologia("2.2.2.2",vizinhos_dois_tcp,vizinhos_dois_udp);

            HashMap<String,Integer> vizinhos_tres_tcp = new HashMap<>();
            vizinhos_tres_tcp.put("2.2.2.2",2222); // Node 2
            vizinhos_tres_tcp.put("4.4.4.4",4444);// Node 4

            HashMap<String,Integer> vizinhos_tres_udp = new HashMap<>();
            vizinhos_tres_udp.put("2.2.2.2",2220); // Node 2
            vizinhos_tres_udp.put("4.4.4.4",4440);// Node 4

            bootstraper.setTopologia("3.3.3.3",vizinhos_tres_tcp,vizinhos_tres_udp);


            HashMap<String,Integer> vizinhos_quatro_tcp = new HashMap<>();
            vizinhos_quatro_tcp.put("3.3.3.3",3333); //
            vizinhos_quatro_tcp.put("6.6.6.6",6666); // RP

            HashMap<String,Integer> vizinhos_quatro_udp = new HashMap<>();
            vizinhos_quatro_udp.put("3.3.3.3",3330); //
            vizinhos_quatro_udp.put("6.6.6.6",6660); // RP

            bootstraper.setTopologia("4.4.4.4",vizinhos_quatro_tcp,vizinhos_quatro_udp);

            HashMap<String,Integer> vizinhos_RP_tcp = new HashMap<>();
            vizinhos_RP_tcp.put("1.1.1.1",1111); // Node1
            vizinhos_RP_tcp.put("4.4.4.4",4444); // Node4
            vizinhos_RP_tcp.put("5.5.5.5",5555); // Bootstraper ou Node5

            HashMap<String,Integer> vizinhos_RP_udp = new HashMap<>();
            vizinhos_RP_udp.put("1.1.1.1",1110); // Node1
            vizinhos_RP_udp.put("4.4.4.4",4440); // Node4
            vizinhos_RP_udp.put("5.5.5.5",5550); // Bootstraper ou Node5

            bootstraper.setTopologia("6.6.6.6",vizinhos_RP_tcp,vizinhos_RP_udp);

    }

    public static void main(String[] args) throws IOException, InterruptedException {

            Node um               = new Node("1.1.1.1",1111,5555,1110);
            Node dois             = new Node("2.2.2.2",2222,5555,2220);
            Node tres             = new Node("3.3.3.3",3333,5555,3330);
            Node quatro           = new Node("4.4.4.4",4444,5555,4440);
            RP seis               = new RP("6.6.6.6",6666,5555,12345,6660);
            Servidor servidor     = new Servidor(12345,12340,6666);
            Bootstraper cinco     = new Bootstraper("5.5.5.5",5555,5550);
            Cliente professorLima = new Cliente("7.7.7.7",2222,"2.2.2.2",7770,7777);

            MakeBootstraper(cinco);

            servidor.inicializador();
            cinco.inicializa();

            Thread.sleep(30);

            //primeira fase
            um.inicializa();
            dois.inicializa();
            tres.inicializa();
            quatro.inicializa();
            seis.inicializa();

            Thread.sleep(30);

            //segunda fase
            um.PedeVizinhos();
            dois.PedeVizinhos();
            tres.PedeVizinhos();
            quatro.PedeVizinhos();
            seis.PedeVizinhos();

            Thread.sleep(30);

            //terceira fase
            um.TudoOK();
            dois.TudoOK();
            tres.TudoOK();
            quatro.TudoOK();
            seis.TudoOK();
            cinco.TudoOK();
    }
}