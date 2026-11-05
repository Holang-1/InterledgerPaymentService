package kwikpay;

import org.interledger.openpayments.httpclient.OpenPaymentsHttpClient;
import org.interledger.openpayments.model.walletaddress.WalletAddress;

import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;

/** Read-only validation of the three wallets for a ZAR Test Wallet split. */
public class SplitPreflight {
    static void verifyWallet(WalletAddress wallet, String requested) {
        if (!requested.equals(wallet.getId()) || !"ZAR".equals(wallet.getAssetCode())
                || !Integer.valueOf(2).equals(wallet.getAssetScale())) {
            throw new IllegalArgumentException("Wallet must resolve to the requested ZAR address with scale 2.");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Provide payer, business income and stock reserve Test Wallet URLs.");
        }
        for (String address : args) WalletLookup.testWallet(address);
        if (List.of(args).stream().distinct().count() != 3) {
            throw new IllegalArgumentException("Use distinct wallet addresses for the payer and both pots.");
        }
        var key = KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPrivate();
        String pem = "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(key.getEncoded()) + "\n-----END PRIVATE KEY-----";
        var client = OpenPaymentsHttpClient.defaultClient(args[0], pem, "preflight-unused");
        String[] labels = {"Payer", "Business income", "Stock reserve"};
        for (int i = 0; i < args.length; i++) {
            var wallet = client.walletAddress().get(args[i]);
            verifyWallet(wallet, args[i]);
            System.out.println(labels[i] + ": " + wallet.getId() + " (ZAR, scale 2)");
        }
        System.out.println("Public wallet preflight passed. No grant or payment was created; separate addresses may share a balance.");
    }
}
