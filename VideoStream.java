import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

public class VideoStream {

    DataInputStream dis; // input stream to read video frames
    int frame_nb; // current frame number

    public VideoStream(String filename) throws Exception {
        // init variables
        File file = new File(filename);
        if (file.exists()) {
            dis = new DataInputStream(new FileInputStream(file));
            frame_nb = 0;
        } else {
            throw new Exception("Video file not found: " + filename);
        }
    }

    // getnextframe
    // returns the next frame as an array of byte and the size of the frame
    public int getnextframe(byte[] frame) throws Exception {
        int length = 0;

        try {
            // read current frame length
            length = dis.readInt();

            // read frame data
            dis.readFully(frame, 0, length);
        } catch (Exception e) {
            // If an exception occurs, assume end of file
            length = -1;
        }

        return length;
    }
}

