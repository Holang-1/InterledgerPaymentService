package kwikpay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.interledger.openpayments.httpclient.OpenPaymentsHttpClient;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

public class WalletLookup {
    static URI testWallet(String address) {
        URI uri = URI.create(address);
        if (!"https".equals(uri.getScheme()) || !"ilp.interledger-test.dev".equals(uri.getHost())
                || uri.getPort() != -1 || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || uri.getPath() == null || uri.getPath().equals("")) {
            throw new IllegalArgumentException("Provide an HTTPS Test Wallet address on ilp.interledger-test.dev.");
        }
        return uri;
    }

    public static void main(String[] args) throws Exception {
        String address = System.getenv("TEST_WALLET_ADDRESS");
        if (address == null || address.isBlank()) throw new IllegalArgumentException("Set TEST_WALLET_ADDRESS.");
        URI wallet = testWallet(address);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8095"));
        var key = KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPrivate();
        String pem = "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(key.getEncoded()) + "\n-----END PRIVATE KEY-----";
        // The SDK requires a signing identity even though wallet discovery is public and unsigned.
        var client = OpenPaymentsHttpClient.defaultClient(wallet.toString(), pem, "poc-unused");
        var mapper = new ObjectMapper();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/wallet", exchange -> {
            int status = 200;
            byte[] body;
            try {
                if (!exchange.getRequestMethod().equals("GET") || !exchange.getRequestURI().getPath().equals("/wallet")) {
                    status = 404;
                    body = mapper.writeValueAsBytes(Map.of("error", "Not found"));
                } else {
                    body = mapper.writeValueAsBytes(client.walletAddress().get(wallet.toString()));
                }
            } catch (Exception e) {
                status = 502;
                body = mapper.writeValueAsBytes(Map.of("error", "Wallet lookup failed"));
                System.err.println("Wallet lookup failed: " + e.getClass().getSimpleName());
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start();
        System.out.println("Wallet lookup listening on http://127.0.0.1:" + server.getAddress().getPort() + "/wallet");
    }
}
