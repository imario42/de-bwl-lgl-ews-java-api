package microsoft.exchange.webservices.data.util;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;

/**
 * An HTTP-Client which handles Proxy-Error in the LGL-Network
 * by resending the request, if the LGL-Proxy is not capable to
 * process the request right away.
 */
public class LglHttpClientRetryHandler {

    /**
     * Bail out, when this amount of retries did not help.
     */
    public static final int MAX_RETRY_COUNT = 7;

    /**
     * Wait this time (milliseconds) between retry attempts.
     */
    public static final int RETRY_WAIT_INTERVAL = 100;

    /**
     * Retry the request, if it fails with one of these problems.
     */
    private static final List<Class<? extends Throwable>> retryableErrors = Arrays.asList(
            ConnectTimeoutException.class,
            ConnectException.class
    );

    public static HttpRequestRetryHandler getHttpRequestRetryHandler() {
        return (exception, executionCount, context) -> {
            // bail out when retries did not help
            if (executionCount >= MAX_RETRY_COUNT) {
                return false;
            }
            // retry on common LGL-Proxy problems
            for (Class<? extends Throwable> retryableError : retryableErrors) {
                if (isExceptionOrCauseRetryable(exception, retryableError)) {
                    waitBetweenRetries();
                    return true;
                }
            }
            // bail out on any other problem
            return false;
        };
    }

    public static ServiceUnavailableRetryStrategy getServiceUnavailableRetryStrategy() {
        return new DefaultServiceUnavailableRetryStrategy(MAX_RETRY_COUNT, RETRY_WAIT_INTERVAL);
    }

    protected static boolean isExceptionOrCauseRetryable(IOException exception, Class<? extends Throwable> retryableError) {
        Throwable cause = exception;
        while (cause != null) {
            if (retryableError.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static void waitBetweenRetries() {
        try {
            Thread.sleep(RETRY_WAIT_INTERVAL);
        } catch (InterruptedException e) {
            // OK, retry comes may be earlier than expected ...
        }
    }
}
