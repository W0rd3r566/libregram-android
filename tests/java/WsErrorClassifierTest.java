import com.libregram.proxy.WsErrorClassifier;

public class WsErrorClassifierTest {
    private static void check(String input, String expected) {
        String actual = WsErrorClassifier.classify(input);
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }
    public static void main(String[] args) {
        check(null, "native_transport_error");
        check("address already in use (os error 98)", "local_port_in_use");
        check("proxy is still stopping, try again", "native_runtime_busy");
        check("invalid certificate for secret-host.example", "tls_error");
        check("DNS failed for secret-host.example", "dns_error");
        check("connection timed out", "network_timeout");
        check("connection refused", "connection_refused");
        check("network is unreachable", "network_unreachable");
        check("unexpected argument --secret sensitive-value", "invalid_configuration");
        check("socks5://user:password@example.com tg://proxy?secret=abcdef", "native_transport_error");
        System.out.println("WS error classification: 10 cases passed; raw input never returned.");
    }
}
