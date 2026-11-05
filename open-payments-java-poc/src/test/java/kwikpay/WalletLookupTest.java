package kwikpay;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WalletLookupTest {
    @Test void onlyAcceptsTestWalletAddresses() {
        assertEquals("ilp.interledger-test.dev", WalletLookup.testWallet("https://ilp.interledger-test.dev/example").getHost());
        for (String address : new String[]{"http://ilp.interledger-test.dev/example", "https://localhost/example", "https://ilp.interledger-test.dev@localhost/example", "https://ilp.interledger-test.dev:443/example", "https://ilp.interledger-test.dev/example?token=x"}) {
            assertThrows(IllegalArgumentException.class, () -> WalletLookup.testWallet(address));
        }
    }
}
