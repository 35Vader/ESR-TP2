import java.io.*;
import java.net.*;
import java.util.Map;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Stream extends JFrame implements ActionListener {

  JLabel label;

  Timer sTimer; // timer usado para enviar as imagens na taxa de quadros do vídeo
  byte[] sBuf; // buffer usado para armazenar as imagens a serem enviadas para o cliente

  AddressingTable table;
  String filename;

  DatagramPacket senddp; // pacote UDP contendo os quadros de vídeo (a serem enviados)
  DatagramSocket RTPsocket; // soquete usado para enviar e receber pacotes UDP
  int RTP_dest_port = 25000; // porta de destino para pacotes RTP
  InetAddress ClientIPAddr; // endereço IP do cliente

  static String VideoFileName; // arquivo de vídeo a ser solicitado ao servidor

  int imagenb = 0; // número da imagem do quadro atualmente transmitido
  VideoStream video; // objeto VideoStream usado para acessar os quadros de vídeo
  static int MJPEG_TYPE = 26; // tipo de carga útil RTP para vídeo MJPEG
  static int FRAME_PERIOD = 100; // período de quadro do vídeo a ser transmitido, em ms
  static int VIDEO_LENGTH = 500; // comprimento do vídeo em quadros


  public Stream(String filename, AddressingTable table) {
    // init Frame
    super("Servidor");

    // init variables
    this.table = table;
    this.filename = filename;

    // Inicialização para a parte do servidor
    sTimer = new Timer(FRAME_PERIOD, this); // Inicializa o Timer para o servidor
    sTimer.setInitialDelay(0);
    sTimer.setCoalesce(true);

    // Inicialização do VideoStream
    sBuf = new byte[15000]; // aloca memória para o buffer de envio

    try {
        RTPsocket = new DatagramSocket(); // Inicializa o soquete RTP
        video = new VideoStream(filename); // Inicializa o objeto VideoStream
        System.out.println("Servidor: vai enviar vídeo do arquivo " + VideoFileName);

    } catch (SocketException e) {
        System.out.println("Servidor: erro no soquete: " + e.getMessage());
    } catch (Exception e) {
        System.out.println(VideoFileName);
        System.out.println("Servidor: erro no vídeo: " + e.getMessage());
    }

    // Handler to close the main window
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        // stop the timer and exit
        sTimer.stop();
        System.exit(0);
      }
    });

    label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);

        sTimer.start();
    }

    public static void execute(String filename, AddressingTable table) {
        File f = new File(filename);
        if (f.exists()) {
            new Stream(filename, table);
        } else {
            VideoFileName = "../files/movie.Mjpeg";
            System.out.println("Servidor: parâmetro não foi indicado. VideoFileName = " + VideoFileName);
            new Stream(VideoFileName, table);
        }
    }

  // Handler for timer
  public void actionPerformed(ActionEvent e) {
    if (imagenb < VIDEO_LENGTH) {
        imagenb++;

        try {
            int image_length = video.getnextframe(sBuf);

            // Adaptação para usar a nova classe RTPpacket
            RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, sBuf, image_length);

            int packet_length = rtp_packet.getLength();
            byte[] packet_bits = new byte[packet_length];
            rtp_packet.getPacket(packet_bits);

            // Envio do pacote para os clientes
            Map<String, Boolean> ips = table.getStreamingTable();
            for (String ip : ips.keySet()) {
                if (ips.get(ip)) {
                    senddp = new DatagramPacket(packet_bits, packet_length, InetAddress.getByName(ip), RTP_dest_port);
                    RTPsocket.send(senddp);
                    System.out.println("Send frame #" + imagenb);
                    rtp_packet.printHeader();
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }

    } else {
        try {
            video = new VideoStream(filename);
            imagenb = 0;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

}

