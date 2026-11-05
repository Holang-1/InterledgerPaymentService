package kwikpay;

import org.interledger.openpayments.httpclient.OpenPaymentsHttpClient;
import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.grant.Grant;
import org.interledger.openpayments.model.grant.Limits;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

/** Checks whether Test Wallet accepts a loopback callback; cancels the pending grant. */
public class ConsentProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Provide a Test Wallet payer address.");
        WalletLookup.testWallet(args[0]);
        String address = IncomingGrant.required("TEST_WALLET_ADDRESS");
        WalletLookup.testWallet(address);
        var client = OpenPaymentsHttpClient.defaultClient(address,
                Files.readString(Path.of(IncomingGrant.required("TEST_WALLET_PRIVATE_KEY_FILE"))),
                IncomingGrant.required("TEST_WALLET_KEY_ID"));
        var payer = client.walletAddress().get(args[0]);
        SplitQuote.verifyEndpoints(payer, args[0]);
        byte[] nonce = new byte[32];
        new SecureRandom().nextBytes(nonce);
        var pending = client.auth().grant().continuation(payer,
                Limits.debit(Amount.build(new BigDecimal("1.00"), "ZAR", 2)),
                URI.create("http://127.0.0.1:8027/callback"), Base64.getUrlEncoder().withoutPadding().encodeToString(nonce));
        if (pending.getInteract() == null || pending.getInteract().getRedirect() == null || pending.getGrantContinue() == null) {
            throw new IllegalStateException("Provider did not return a pending interactive grant.");
        }
        var grant = new Grant();
        grant.setAccessContinue(pending.getGrantContinue());
        client.auth().grant().cancel(grant);
        System.out.println("Test Wallet accepted the loopback callback and the pending grant was cancelled. No payment was authorized.");
    }
}
