package info.ragozin.loadscript;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper;

public class LoggingWebConnection extends WebConnectionWrapper {
	private static final double NANOS = TimeUnit.MILLISECONDS.toNanos(1);

	public LoggingWebConnection(WebConnection webConnection) throws IllegalArgumentException {
		super(webConnection);
	}

	@Override
	public WebResponse getResponse(WebRequest request) throws IOException {
		String method = request.getHttpMethod().toString();
		String url = request.getUrl().toString();
		long time = System.nanoTime();
		long beginAt = System.currentTimeMillis();
		String code = "fail";
		try {
			WebResponse response = super.getResponse(request);
			code = String.valueOf(response.getStatusCode());
			return response;
		}
		finally {
			long dur = System.nanoTime() - time;
			System.out.printf("[%d][%s] at %3.1fms - (%s) %s%n", beginAt, method, dur / NANOS, code, url);
		}
	}
}
