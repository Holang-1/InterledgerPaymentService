package kwikpay;

import org.interledger.openpayments.httpclient.OpenPaymentsHttpClient;
import org.interledger.openpayments.model.grant.AccessAction;
import org.interledger.openpayments.model.quote.Quote;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Creates Test Wallet incoming payments and quotes; never requests consent or sends funds. */
public class SplitQuote {
    static void verifyEndpoints(WalletAddress wallet, String address) {
        SplitPreflight.verifyWallet(wallet, address);
        for (String endpoint : List.of(wallet.getAuthServer(), wallet.getResourceServer())) {
            URI uri = URI.create(endpoint);
            if (!"https".equals(uri.getScheme()) || !("auth.interledger-test.dev".equals(uri.getHost())
                    || "ilp.interledger-test.dev".equals(uri.getHost())) || uri.getPort() != -1
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException("Unexpected Test Wallet provider endpoint.");
            }
        }
    }

    static BigDecimal debit(Quote quote) {
        var amount = quote.getDebitAmount();
        if (amount == null || !"ZAR".equals(amount.getAssetCode()) || amount.getAssetScale() != 2
                || amount.getValue() == null || !amount.getValue().matches("[0-9]+")) {
            throw new IllegalArgumentException("Provider returned an invalid ZAR quote.");
        }
        BigDecimal value = new BigDecimal(amount.getValue()).movePointLeft(2);
        if (value.signum() <= 0) throw new IllegalArgumentException("Quote must be positive.");
        return value;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Provide payer, business and stock Test Wallet URLs.");
        for (String address : args) WalletLookup.testWallet(address);
        if (List.of(args).stream().distinct().count() != 3) throw new IllegalArgumentException("Wallet addresses must be distinct.");
        String clientAddress = IncomingGrant.required("TEST_WALLET_ADDRESS");
        WalletLookup.testWallet(clientAddress);
        String keyId = IncomingGrant.required("TEST_WALLET_KEY_ID");
        String privateKey = Files.readString(Path.of(IncomingGrant.required("TEST_WALLET_PRIVATE_KEY_FILE")));
        var client = OpenPaymentsHttpClient.defaultClient(clientAddress, privateKey, keyId);
        WalletAddress[] wallets = new WalletAddress[3];
        for (int i = 0; i < args.length; i++) {
            wallets[i] = client.walletAddress().get(args[i]);
            verifyEndpoints(wallets[i], args[i]);
        }
        var quoteGrant = client.auth().grant().quote(wallets[0], AccessAction.create);
        if (quoteGrant.getAccess() == null || quoteGrant.getAccess().getToken() == null) throw new IllegalStateException("Quote grant missing access token.");
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 1; i < 3; i++) {
            var grant = client.auth().grant().incomingPayment(wallets[i], AccessAction.create);
            var incoming = client.payment().createIncoming(wallets[i], grant, i == 1 ? new BigDecimal("0.80") : new BigDecimal("0.20"));
            var quote = client.quote().create(quoteGrant.getAccess().getToken(), wallets[0], incoming, Optional.empty(), Optional.empty());
            BigDecimal amount = debit(quote);
            total = total.add(amount);
            System.out.println((i == 1 ? "Business" : "Stock") + " quoted debit: ZAR " + amount);
        }
        System.out.println("Total quoted payer debit: ZAR " + total + ". No payment was authorized or sent.");
    }
}
