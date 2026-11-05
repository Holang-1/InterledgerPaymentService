package kwikpay;

import org.interledger.openpayments.model.walletaddress.WalletAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SplitPreflightTest {
    @Test
    void requiresMatchingZarWalletAndScale() {
        var wallet = new WalletAddress();
        wallet.setId("https://ilp.interledger-test.dev/test");
        wallet.setAssetCode("ZAR");
        wallet.setAssetScale(2);
        assertDoesNotThrow(() -> SplitPreflight.verifyWallet(wallet, wallet.getId()));
        assertThrows(IllegalArgumentException.class, () -> SplitPreflight.verifyWallet(wallet, "https://ilp.interledger-test.dev/other"));
        wallet.setAssetCode("USD");
        assertThrows(IllegalArgumentException.class, () -> SplitPreflight.verifyWallet(wallet, wallet.getId()));
        wallet.setAssetCode("ZAR");
        wallet.setAssetScale(3);
        assertThrows(IllegalArgumentException.class, () -> SplitPreflight.verifyWallet(wallet, wallet.getId()));
    }
}
