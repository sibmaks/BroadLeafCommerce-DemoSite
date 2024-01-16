package info.ragozin.loadscript;

import java.io.IOException;
import java.net.URL;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;

public class HostOverridingConnection implements WebConnection {

    private final String connectiontarget;
    private final WebConnection delegate;

    public HostOverridingConnection(String connectiontarget, WebConnection delegate) {
        this.connectiontarget = connectiontarget;
        this.delegate = delegate;
    }

    @Override
    public WebResponse getResponse(WebRequest request) throws IOException {
        URL url = request.getUrl();
        String nurl = connectiontarget;
        if (url.getFile() != null) {
            nurl += url.getFile();
        }

        request.setUrl(new URL(nurl));

        return delegate.getResponse(request);
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }
}
