package info.ragozin.demostarter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

public class ContainerHelper {

    private static String dockerCmd = "docker";
    private static Boolean dockerPresent = null;

    public static boolean isDockerAvailable() {
        if (dockerPresent != null) {
            return dockerPresent;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(dockerCmd, "--version");
            pb.directory(new File(DemoInitializer.getDemoHome()));
            Process p = pb.start();
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroy();
                System.out.println("Timeout running docker command");
                dockerPresent = Boolean.FALSE;
                return false;
            }

            int code = p.exitValue();
            if (code == 0) {
                String version = IOUtils.toString(p.getInputStream());
                System.out.println("Docker version: " + version);
                dockerPresent = Boolean.TRUE;
            } else {
                System.out.println("Docker command is not available");
                dockerPresent = Boolean.FALSE;
            }
        } catch (IOException e) {
            System.out.println("Error running docker command: " + e.toString());
            dockerPresent = Boolean.FALSE;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return dockerPresent.booleanValue();
    }

    public static boolean checkRunning(String containerName) {
        if (!isDockerAvailable()) {
            return false;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(dockerCmd, "ps");
            pb.directory(new File(DemoInitializer.getDemoHome()));
            Process p = pb.start();
            if (!p.waitFor(10, TimeUnit.SECONDS) || p.exitValue() != 0) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    // ignore
                }
                throw new RuntimeException("docker ps is not succesful");
            }
            ;
            String output = IOUtils.toString(p.getInputStream());
            System.out.println("ps output:\n" + output);
            String[] lines = output.split("[\\n]");
            for (String line: lines) {
                String[] parts = line.split("\\s+");
                System.out.println("Check line: " + Arrays.toString(parts));
                String last = parts.length == 0 ? "" : parts[parts.length - 1];
                if (containerName.equals(last)) {
                    return true;
                }
            }

            return false;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String showCommand(ProcessBuilder pb) {
        StringBuilder sb = new StringBuilder();
        for (String cp: pb.command()) {
            sb.append(escapeCmd(cp)).append(' ');
        }
        return sb.toString();
    }

    private static Object escapeCmd(String cp) {
        if (cp.indexOf(' ') >= 0) {
            return "\"" + cp + "\"";
        } else {
            return cp;
        }
    }

    public static void stopContainer(String containerName) {

        try {
            ProcessBuilder pb = new ProcessBuilder(dockerCmd, "stop", containerName);
            pb.directory(new File(DemoInitializer.getDemoHome()));
            pb.inheritIO();
            System.out.println("Container command: " + showCommand(pb));
            pb.start().waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeContainer(String containerName) {

        try {
            ProcessBuilder pb = new ProcessBuilder(dockerCmd, "rm", containerName);
            pb.directory(new File(DemoInitializer.getDemoHome()));
            pb.inheritIO();
            System.out.println("Container command: " + showCommand(pb));
            pb.start().waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Builder builder(String name, String image) {
        Builder bld = new Builder();
        bld.image = image;
        bld.cmd.addAll(Arrays.asList(dockerCmd, "run", "-d", "--name", name, "--add-host", "host.docker.internal:host-gateway"));
        return bld;
    }

    public static class Builder {

        List<String> cmd = new ArrayList<String>();
        String image;

        public Builder mount(String src, String dst) {
            cmd.addAll(Arrays.asList("-v", src + ":" + dst));
            return this;
        }

        public Builder port(int inside, int outside) {
            cmd.addAll(Arrays.asList("-p", inside + ":" + outside + "/tcp"));
            return this;
        }

        public void run() {
            try {
                List<String> cmd = new ArrayList<>(this.cmd);
                cmd.add(image);
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(new File(DemoInitializer.getDemoHome()));
                pb.inheritIO();
                System.out.println("Container command: " + showCommand(pb));
                pb.start().waitFor(30, TimeUnit.SECONDS);
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
