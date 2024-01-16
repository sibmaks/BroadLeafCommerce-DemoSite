package info.ragozin.demostarter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang3.SystemUtils;

public class ProcessWatchDog extends Thread {

    private static InetAddress LOCALHOST;
    static {
        try {
            System.out.println("MacOS is detected use 127.0.0.1 for life grant");
            if (SystemUtils.IS_OS_MAC) {
                // MacOS has limited loopback addresses
                LOCALHOST = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
            } else {
                LOCALHOST = InetAddress.getByAddress(new byte[] {127, 0, 0, 42});
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean check(File lifeGrant) {
        try {
            Grant g = open(lifeGrant);
            if (g == null) {
                return false;
            }
            Socket sock = new Socket(g.host, g.port);
            if (sock.isConnected()) {
                sock.close();
                return true;
            }
            else {
                sock.close();
                return false;
            }
        }
        catch(IOException e) {
            return false;
        }
    }

    public static void kill(File lifeGrant) {
        try {
            Grant g = open(lifeGrant);
            if (g != null) {
                Socket sock = new Socket(g.host, g.port);
                if (sock.isConnected()) {
                    sock.getOutputStream().write(g.magic.getBytes());
                    sock.setSoTimeout(10 * 60 * 1000);
                    sock.getInputStream().read(); // socket should be closed by process shutdown
                }
                else {
                    sock.close();
                }
            }
        }
        catch(IOException e) {
        }
    }

    private static Grant open(File lifeGrant) throws IOException {
        if (lifeGrant.isFile()) {
            byte[] data = new byte[4 << 10];
            FileInputStream fis = new FileInputStream(lifeGrant);
            int n = fis.read(data);
            fis.close();
            String token = new String(data, 0, n);

            Grant g = new Grant();
            int ch = token.indexOf('\n');
            g.vmname = ch < 0 ? token : token.substring(0, ch);
            if (ch > 0) {
                int sp = token.lastIndexOf(' ');
                String sock = token.substring(ch + 1, sp);
                if (sock.indexOf(':') >= 0) {
                    g.host = InetAddress.getByName(sock.substring(0, sock.indexOf(':')));
                    g.port = Integer.valueOf(sock.substring(sock.indexOf(':') + 1));
                } else {
                    g.host = LOCALHOST;
                    g.port = Integer.valueOf(token.substring(ch + 1, sp));
                }

                g.magic = token.substring(sp + 1);
            }
            return g;
        }
        else {
            return null;
        }
    }

    private static class Grant {
        @SuppressWarnings("unused")
        String vmname;
        InetAddress host;
        int port;
        String magic;
    }

    private final File lifeGrant;
    private final String vmname;
    private final ServerSocket socket;
    private final String magic = UUID.randomUUID().toString();

    public ProcessWatchDog(File lifeGrant) {
        try {
            if (lifeGrant.getParentFile() != null) {
                lifeGrant.getParentFile().mkdirs();
            }
            setDaemon(true);
            setName("ProcessWatchDog");
            vmname = ManagementFactory.getRuntimeMXBean().getName();
            this.lifeGrant = lifeGrant;
            if (lifeGrant.isFile()) {
                kill(lifeGrant);
            }
            lifeGrant.delete();
            if (lifeGrant.exists()) {
                throw new RuntimeException("Cannot remove life grant file: " + lifeGrant.getPath());
            }
            FileOutputStream fos = new FileOutputStream(lifeGrant);
            int lgport = Integer.getInteger("lifeGrant.port", 0);
            String token;
            if (lgport == 0) {
                socket = new ServerSocket(lgport, 10, LOCALHOST);
                token = vmname + "\n" + socket.getLocalPort() + " " + magic;
            } else {
                System.out.println("Using explict life grant, port: " + lgport);
                // bind to all addresses
                socket = new ServerSocket(lgport, 10);
                token = vmname + "\n" + "127.0.0.1:" + socket.getLocalPort() + " " + magic;
            }
            fos.write(token.getBytes());
            fos.close();
            start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            while(true) {
                try {
                    socket.setSoTimeout(400);
                    Socket sock = socket.accept();
                    if (verifyMagic(sock)) {
                        break;
                    }
                } catch (IOException e) {
                    // ignore
                }
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lifeGrant)));
                    String id = br.readLine();
                    br.close();
                    if (!vmname.equals(id)) {
                        break;
                    }
                }
                catch(IOException e) {
                    // ignore
                    break;
                }
            }
        }
        finally {
            System.err.println("Life grant revoked, terminating ...");
            System.err.flush();
            System.out.flush();
            Runtime.getRuntime().halt(0);
        }
    }

    private boolean verifyMagic(Socket sock) throws IOException {
        byte[] a = magic.getBytes();
        byte[] b = new byte[a.length];
        sock.setSoTimeout(500);
        int n = 0;
        while(n < a.length) {
            int m = sock.getInputStream().read(b, n, b.length - n);
            if (m < 0) {
                return false;
            }
            n += m;
        }
        return Arrays.equals(a, b);
    }
}
