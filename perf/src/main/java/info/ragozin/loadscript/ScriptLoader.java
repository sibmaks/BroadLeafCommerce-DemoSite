package info.ragozin.loadscript;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class ScriptLoader {

    public static List<LoadScriptStep> loadScript(String path) throws IOException {
        List<LoadScriptStep> steps = new ArrayList<>();

        List<String> files = list(path);
        Collections.sort(files);
        for(String f: files) {
            System.out.println("Load step: " + f);
            steps.add(loadStep(path + "/" + f));
        }

        return steps;
    }

    private static RecorderScriptStep loadStep(String path) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        RecorderScriptStep step = new RecorderScriptStep(is);
        step.add(new PageAssetFetchProcessor());
        return step;
    }

    private static List<String> list(String path) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(Thread.currentThread().getContextClassLoader());
        List<String> result = new ArrayList<>();
        for (Resource r: resolver.getResources(path + "/*")) {
            result.add(r.getFilename());
        };
        return result;
//        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
//        BufferedReader bt = new BufferedReader(new InputStreamReader(is));
//        List<String> files = new ArrayList<>();
//        while(true) {
//            String line = bt.readLine();
//            if (line == null) {
//                break;
//            }
//            files.add(line);
//        }
//        bt.close();
//        return files;
    }
}
