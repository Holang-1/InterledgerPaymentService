package kwikpay;

import org.interledger.openpayments.httpclient.OpenPaymentsHttpClient;
import org.interledger.openpayments.model.grant.AccessAction;

import java.nio.file.Files;
import java.nio.file.Path;

public class IncomingGrant {
    public static void main(String[] args) throws Exception {
        String address = required("TEST_WALLET_ADDRESS");
        String keyId = required("TEST_WALLET_KEY_ID");
        Path keyPath = Path.of(required("TEST_WALLET_PRIVATE_KEY_FILE"));
        WalletLookup.testWallet(address);
        var client = OpenPaymentsHttpClient.defaultClient(address, Files.readString(keyPath), keyId);
        var wallet = client.walletAddress().get(address);
        var grant = client.auth().grant().incomingPayment(wallet, AccessAction.create);
        if (grant.getAccess() == null || grant.getAccess().getToken() == null || grant.getAccess().getToken().isBlank()) {
            throw new IllegalStateException("No access token received; check provider authorization.");
        }
        System.out.println("Authenticated incoming-payment create grant received. No payment was created.");
    }

    static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + name + ".");
        return value;
    }
}
