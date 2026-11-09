package com.libregram.proxy;

import java.util.Locale;

/** Returns only fixed public codes, never native input, hosts, URLs or credentials. */
public final class WsErrorClassifier {
    private WsErrorClassifier() { }
    public static String classify(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (text.contains("already running") || text.contains("still stopping")) return "native_runtime_busy";
        if (text.contains("address already in use") || text.contains("os error 98")) return "local_port_in_use";
        if (text.contains("permission denied")) return "permission_denied";
        if (text.contains("certificate") || text.contains("invalid peer") || text.contains("tls")) return "tls_error";
        if (text.contains("dns") || text.contains("name resolution") || text.contains("failed to lookup")) return "dns_error";
        if (text.contains("timed out") || text.contains("timeout")) return "network_timeout";
        if (text.contains("connection refused")) return "connection_refused";
        if (text.contains("network is unreachable") || text.contains("no route to host")) return "network_unreachable";
        if (text.contains("invalid") || text.contains("unexpected argument") || text.contains("required argument")) return "invalid_configuration";
        return "native_transport_error";
    }
}
