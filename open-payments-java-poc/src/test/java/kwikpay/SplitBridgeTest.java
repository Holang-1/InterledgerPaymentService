package kwikpay;

import java.time.Instant;
import org.interledger.openpayments.exception.OpenPaymentsAPIException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SplitBridgeTest {
    @Test void callbackAcceptsOnlyTrustedOriginsAndExecutionIds() {
        var execution = "exec_" + "a".repeat(32);
        assertEquals("https://demo.up.railway.app/api/test-callback/" + execution, SplitBridge.callback("https://demo.up.railway.app", execution).toString());
        assertThrows(IllegalArgumentException.class, () -> SplitBridge.callback("https://demo.up.railway.app.attacker.test", execution));
        assertThrows(IllegalArgumentException.class, () -> SplitBridge.callback("https://demo.up.railway.app", "../other"));
    }
    @Test void expiryIsCheckedAtTheSendBoundary() {
        Instant now = Instant.parse("2026-10-31T09:10:46Z");
        assertThrows(SplitBridge.QuoteExpiredException.class, () -> SplitBridge.ensureQuoteUsable(null, now));
        assertThrows(SplitBridge.QuoteExpiredException.class, () -> SplitBridge.ensureQuoteUsable(now, now));
        assertThrows(SplitBridge.QuoteExpiredException.class, () -> SplitBridge.ensureQuoteUsable(now.minusSeconds(1), now));
        assertDoesNotThrow(() -> SplitBridge.ensureQuoteUsable(now.plusSeconds(1), now));
    }

    @Test void errorDetailsExcludeProviderMessagesAndCredentials() {
        var error = new OpenPaymentsAPIException(503, "private token and provider body");
        assertEquals("KWIKPAY_BRIDGE_ERROR provider_error 503", SplitBridge.errorDetails(error));
        assertEquals("KWIKPAY_BRIDGE_ERROR quote_expired -", SplitBridge.errorDetails(new SplitBridge.QuoteExpiredException()));
        assertFalse(SplitBridge.errorDetails(error).contains("private"));
    }
}
