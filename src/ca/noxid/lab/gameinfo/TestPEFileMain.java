package ca.noxid.lab.gameinfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;

/**
 * Do not use this as the main class!
 * Exists to just run quick tests on PE handling code.
 */
public class TestPEFileMain {
    public static void main(String[] args) throws IOException {
        FileInputStream inStream = new FileInputStream(args[0]);
        FileChannel chan = inStream.getChannel();
        long l = chan.size();
        if (l > 0x7FFFFFFF)
            throw new IOException("Too big!");
        ByteBuffer bb = ByteBuffer.allocate((int) l);
        if (chan.read(bb) != l)
            throw new IOException("Didn't read whole file.");
        chan.close();

        PEFile pef = new PEFile(bb, 0x1000);
        FileOutputStream fos = new FileOutputStream(args[0] + ".out.exe");
        fos.write(pef.write());
        fos.close();
    }
}
