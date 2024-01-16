package info.ragozin.demo;

import org.junit.Test;

import info.ragozin.loadgen.LoadGenStarter;

public class DockerDemoStartAndLoad {

    @Test
    public void startDemoAndLoad() throws InterruptedException {
        new DockerDemoStarter().startDemo();

        System.out.println("");
        System.out.println("Starting load ...");
        LoadGenStarter.start();
        System.out.println("");
        System.out.println("System is under load now");
    }
}
