package info.ragozin.loadscript;

import info.ragozin.demostarter.DemoInitializer;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoadGeneratorCheck {

    @Test
    public void go() throws IOException, InterruptedException, SAXException {

        List<LoadScriptStep> steps = ScriptLoader.loadScript("load-scripts/check-script");

        LoadScriptExecutor executor = new LoadScriptExecutor(steps);

        Thread currentThread = Thread.currentThread();
        while (!currentThread.isInterrupted()) {
            executor.perform();
//			break;
        }
    }

    @Test
    public void go_mt() throws IOException, InterruptedException {

        int sessions = DemoInitializer.propAsInt("loadgen.users", 40);

        List<LoadScriptStep> steps = ScriptLoader.loadScript("load-scripts/check-script");

        System.out.println("Staring " + sessions + " users, with " + steps.size() + " steps long script");
        Executor service = createRandomDelayExecutor(15);

        Random rnd = new Random(1);
        for (int i = 0; i != sessions; ++i) {
            int delay = rnd.nextInt(5);
            Thread.sleep(delay + 1000);
            startSession(service, steps);
        }

        Thread currentThread = Thread.currentThread();
        while (!currentThread.isInterrupted()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    private Executor createRandomDelayExecutor(int threads) {
        final Random rnd = new Random();
        final ScheduledExecutorService schedule = Executors.newScheduledThreadPool(threads);
        return command -> schedule.schedule(command, rnd.nextInt(1000), TimeUnit.MILLISECONDS);
    }

    private void startSession(Executor service, List<LoadScriptStep> steps) {

        LoadScriptExecutor executor = new LoadScriptExecutor(steps);
        //executor.setTargetURL("http://192.168.1.103:8080");

        System.out.println("Starting bot ...");
        executor.perform(service, new Runnable() {

            @Override
            public void run() {

                startSession(service, steps);
            }
        });
    }
}
