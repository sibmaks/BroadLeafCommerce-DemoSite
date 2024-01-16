package info.ragozin.demo;

import org.junit.Test;

import info.ragozin.demostarter.ContainerHelper;
import info.ragozin.demostarter.DemoInitializer;
import info.ragozin.hsql.HsqlStarter;
import info.ragozin.site.SiteStarter;
import info.ragozin.solr.SolrStarter;

public class DockerDemoStarter {

    @Test
    public void startDemo() throws InterruptedException {

        DemoInitializer.initConfiguration();

        if (!ContainerHelper.isDockerAvailable()) {
            System.out.println("Docker command is not available, unable to start contenerized environbment");
        }

        if (!HsqlStarter.check()) {
            System.out.println("Starting HSQL");
            HsqlStarter.start();
        }
        else {
            System.out.println("Already started HSQL");
        }

        if (!DemoInitializer.check("solr")) {
            System.out.println("Starting Solr");
            SolrStarter.provisionAndStartSolr();
        }
        else {
            System.out.println("Already started Solr");
        }

        if (SiteStarter.checkPort()) {
            System.out.println("Port 8080 is active");
            if (!ContainerHelper.checkRunning("storefront")) {
                System.out.println("Container is not running, try to kill host process");
                DemoInitializer.kill("storefront");
            }
        }

        if (!ContainerHelper.checkRunning("storefront")) {
            System.out.println("Starting Spring Boot app in container ...");
            ContainerHelper.builder("storefront", "boot-community-demo-site:1.0.0-SNAPSHOT")
                .mount("pid", "/pid")
                .mount("var", "/var")
                .port(8080, 8080)
                .port(11222, 11222) // spare port for JMX
                .port(23045, 23045) // static port for live grant
                .run();
        }

        System.out.println("Waiting for 127.0.0.1:8080");
        SiteStarter.waitForPort(8080);

        System.out.println("");
        System.out.println("Now you can start application dokcer image");
        System.out.println("Then go to http://localhost:8080");
        System.out.println("");
        System.out.println("Remove \"pids\" directory to stop demo enviroment");
        System.out.println("Use \"docker rm -f storefront\" to remove container");
        System.out.println("");
        System.out.println("");
    }
}
