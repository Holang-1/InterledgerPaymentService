package kwikpay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.interledger.openpayments.httpclient.OpenPaymentsHttpClient;
import org.interledger.openpayments.mapper.OpenPaymentsObjectMapper;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.exception.AOpenPaymentsException;
import org.interledger.openpayments.model.grant.AccessAction;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.model.grant.Limits;
import org.interledger.openpayments.model.grant.PendingGrant;
import org.interledger.openpayments.model.quote.Quote;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/** Private, single-request SDK bridge. Caller must keep its input and output server-side. */
public class SplitBridge {
    private static final ObjectMapper JSON = OpenPaymentsObjectMapper.get();

    static WalletAddress wallet(org.interledger.openpayments.IOpenPaymentsClient client, String address) {
        WalletLookup.testWallet(address);
        var result = client.walletAddress().get(address);
        SplitQuote.verifyEndpoints(result, address);
        return result;
    }

    static String text(JsonNode node, String name) {
        var value = node.get(name);
        if (value == null || !value.isTextual() || value.textValue().isBlank() || value.textValue().length() > 4096) throw new IllegalArgumentException("Missing or invalid " + name);
        return value.textValue();
    }

    static class QuoteExpiredException extends RuntimeException {}

    static URI callback(String origin, String execution) {
        URI base = URI.create(origin);
        boolean local = "http".equals(base.getScheme()) && "127.0.0.1".equals(base.getHost()) && base.getPort() == 8027;
        boolean hosted = "https".equals(base.getScheme()) && base.getHost() != null && base.getHost().endsWith(".up.railway.app") && base.getPort() == -1;
        if ((!local && !hosted) || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null || !(base.getPath().isEmpty() || base.getPath().equals("/"))) throw new IllegalArgumentException("Invalid callback origin.");
        if (!execution.matches("exec_[0-9a-f]{32}")) throw new IllegalArgumentException("Invalid execution ID.");
        return base.resolve("/api/test-callback/" + execution);
    }

    static void ensureQuoteUsable(Instant expiresAt, Instant now) {
        if (expiresAt == null || !expiresAt.isAfter(now)) throw new QuoteExpiredException();
    }

    static String errorDetails(Exception e) {
        String category = e instanceof QuoteExpiredException ? "quote_expired" : e instanceof AOpenPaymentsException ? "provider_error" : e instanceof IllegalArgumentException ? "validation_error" : "bridge_error";
        int code = e instanceof AOpenPaymentsException api ? api.getCode() : 0;
        return "KWIKPAY_BRIDGE_ERROR " + category + " " + (code >= 100 && code <= 599 ? code : "-");
    }

    public static void main(String[] args) {
        try {
            JsonNode input = JSON.readTree(System.in);
            String clientAddress = IncomingGrant.required("TEST_WALLET_ADDRESS");
            WalletLookup.testWallet(clientAddress);
            var client = OpenPaymentsHttpClient.defaultClient(clientAddress,
                    Files.readString(Path.of(IncomingGrant.required("TEST_WALLET_PRIVATE_KEY_FILE"))), IncomingGrant.required("TEST_WALLET_KEY_ID"));
            String action = text(input, "action");
            Object result;
            switch (action) {
                case "prepare" -> {
                    var legs = input.get("legs");
                    if (legs == null || !legs.isArray() || legs.size() != 2) throw new IllegalArgumentException("Two legs are required.");
                    String payerAddress = text(input, "payerWalletAddress");
                    var payer = wallet(client, payerAddress);
                    var quoteGrant = client.auth().grant().quote(payer, AccessAction.create);
                    if (quoteGrant.getAccess() == null || quoteGrant.getAccess().getToken() == null) throw new IllegalStateException("Missing quote grant.");
                    var quotes = new ArrayList<Quote>();
                    var total = BigDecimal.ZERO;
                    for (int i = 0; i < 2; i++) {
                        String destination = text(legs.get(i), "walletAddress");
                        String minor = text(legs.get(i), "amountMinor");
                        if (!minor.matches("[0-9]{1,6}") || new BigDecimal(minor).signum() <= 0) throw new IllegalArgumentException("Invalid leg amount.");
                        var recipient = wallet(client, destination);
                        var grant = client.auth().grant().incomingPayment(recipient, AccessAction.create);
                        var incoming = client.payment().createIncoming(recipient, grant, new BigDecimal(minor).movePointLeft(2));
                        var quote = client.quote().create(quoteGrant.getAccess().getToken(), payer, incoming, Optional.empty(), Optional.empty());
                        if (!incoming.getId().equals(quote.getReceiver()) || !URI.create(payerAddress).equals(quote.getWalletAddress())) throw new IllegalStateException("Quote does not match requested wallet.");
                        var receive = quote.getReceiveAmount();
                        if (receive == null || !"ZAR".equals(receive.getAssetCode()) || receive.getAssetScale() != 2 ||
                                !(new BigDecimal(minor).compareTo(new BigDecimal(receive.getValue())) == 0)) throw new IllegalStateException("Unexpected quoted receive amount.");
                        total = total.add(SplitQuote.debit(quote));
                        quotes.add(quote);
                    }
                    result = Map.of("quotes", quotes, "debit", total.toPlainString(), "authServer", payer.getAuthServer());
                }
                case "consent" -> {
                    String payerAddress = text(input, "payerWalletAddress");
                    var payer = wallet(client, payerAddress);
                    String value = text(input, "debitMinor");
                    if (!value.matches("[0-9]{1,6}") || new BigDecimal(value).signum() <= 0) throw new IllegalArgumentException("Invalid debit limit.");
                    var limit = new Amount();
                    limit.setAssetCode("ZAR"); limit.setAssetScale(2); limit.setValue(value);
                    String nonce = text(input, "nonce");
                    if (!nonce.matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException("Invalid callback nonce.");
                    String execution = text(input, "executionId");
                    if (!execution.matches("exec_[0-9a-f]{32}")) throw new IllegalArgumentException("Invalid execution ID.");
                    URI callback = callback(System.getenv().getOrDefault("KWIKPAY_CALLBACK_ORIGIN", "http://127.0.0.1:8027"), execution);
                    var pending = client.auth().grant().continuation(payer, Limits.debit(limit), callback, nonce);
                    URI redirect = pending.getInteract().getRedirect();
                    if (redirect == null || !"https".equals(redirect.getScheme()) || !"auth.interledger-test.dev".equals(redirect.getHost()) || redirect.getUserInfo() != null || redirect.getPort() != -1) throw new IllegalStateException("Unexpected consent URL.");
                    result = Map.of("pending", pending, "redirect", redirect.toString());
                }
                case "cancel" -> {
                    var pending = JSON.treeToValue(input.get("pending"), PendingGrant.class);
                    URI uri = pending.getGrantContinue().getUri();
                    if (!"https".equals(uri.getScheme()) || !"auth.interledger-test.dev".equals(uri.getHost())) throw new IllegalArgumentException("Invalid continuation endpoint.");
                    var grant = new Grant();
                    grant.setAccessContinue(pending.getGrantContinue());
                    client.auth().grant().cancel(grant);
                    result = Map.of("cancelled", true);
                }
                case "continue" -> {
                    var pending = JSON.treeToValue(input.get("pending"), PendingGrant.class);
                    URI uri = pending.getGrantContinue().getUri();
                    if (!"https".equals(uri.getScheme()) || !"auth.interledger-test.dev".equals(uri.getHost()) || uri.getUserInfo() != null || uri.getPort() != -1) throw new IllegalArgumentException("Invalid continuation endpoint.");
                    result = client.auth().grant().finalize(pending, text(input, "interactRef"));
                }
                case "send" -> {
                    var grant = JSON.treeToValue(input.get("grant"), Grant.class);
                    var quote = JSON.treeToValue(input.get("quote"), Quote.class);
                    String payerAddress = text(input, "payerWalletAddress");
                    var payer = wallet(client, payerAddress);
                    if (quote.getId() == null || !"ilp.interledger-test.dev".equals(quote.getId().getHost()) || !URI.create(payerAddress).equals(quote.getWalletAddress())) throw new IllegalArgumentException("Invalid quote.");
                    ensureQuoteUsable(quote.getExpiresAt(), Instant.now());
                    result = client.payment().createOutgoing(grant, payer, quote);
                }
                case "incoming" -> {
                    String destination = text(input, "destination");
                    URI incomingUrl = WalletLookup.testWallet(text(input, "incomingUrl"));
                    var recipient = wallet(client, destination);
                    var readGrant = client.auth().grant().incomingPayment(recipient, AccessAction.read);
                    var incoming = client.payment().getIncoming(incomingUrl, readGrant);
                    if (!URI.create(destination).equals(incoming.getWalletAddress()) || !incomingUrl.equals(incoming.getId()))
                        throw new IllegalStateException("Incoming payment does not match the destination.");
                    result = incoming;
                }
                case "poll" -> {
                    var grant = JSON.treeToValue(input.get("grant"), Grant.class);
                    URI uri = URI.create(text(input, "outgoingUrl"));
                    if (!"https".equals(uri.getScheme()) || !"ilp.interledger-test.dev".equals(uri.getHost()) || uri.getUserInfo() != null || uri.getPort() != -1 || uri.getQuery() != null || uri.getFragment() != null) throw new IllegalArgumentException("Invalid outgoing URL.");
                    var outgoing = client.payment().getOutgoing(uri, grant);
                    String destination = text(input, "destination");
                    var quote = JSON.treeToValue(input.get("quote"), Quote.class);
                    if (quote.getReceiver() == null || !"https".equals(quote.getReceiver().getScheme()) ||
                            !"ilp.interledger-test.dev".equals(quote.getReceiver().getHost()) ||
                            !quote.getId().equals(outgoing.getQuoteId())) throw new IllegalArgumentException("Outgoing payment does not match the quote.");
                    var recipient = wallet(client, destination);
                    var readGrant = client.auth().grant().incomingPayment(recipient, AccessAction.read);
                    var incoming = client.payment().getIncoming(quote.getReceiver(), readGrant);
                    if (!URI.create(destination).equals(incoming.getWalletAddress()) || !quote.getReceiver().equals(incoming.getId()))
                        throw new IllegalStateException("Incoming payment does not match the destination.");
                    result = Map.of("outgoing", outgoing, "incoming", incoming);
                }
                default -> throw new IllegalArgumentException("Unknown action.");
            }
            System.out.println(JSON.writeValueAsString(result));
        } catch (Exception e) {
            System.err.println(errorDetails(e));
            System.exit(e instanceof QuoteExpiredException ? 42 : 1);
        }
    }
}
