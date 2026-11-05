package kwikpay;

import org.interledger.openpayments.model.common.Amount;
import org.interledger.openpayments.model.quote.Quote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SplitQuoteTest {
    @Test void verifiesProviderDebitAmounts() {
        var amount = new Amount();
        amount.setAssetCode("ZAR");
        amount.setAssetScale(2);
        amount.setValue("80");
        var quote = new Quote();
        quote.setDebitAmount(amount);
        assertEquals(new BigDecimal("0.80"), SplitQuote.debit(quote));
        amount.setValue("-1");
        assertThrows(IllegalArgumentException.class, () -> SplitQuote.debit(quote));
        amount.setValue("0");
        assertThrows(IllegalArgumentException.class, () -> SplitQuote.debit(quote));
        amount.setValue("80");
        amount.setAssetCode("USD");
        assertThrows(IllegalArgumentException.class, () -> SplitQuote.debit(quote));
    }
}
