# Open Payments Java POC

This standalone project contains a public wallet lookup service and Test Wallet experiments using the Java Open Payments SDK. The lookup is read-only; the other commands can create provider resources or request authorization, as described below. The SplitBridge is used by the KwikPay Test Wallet API to execute payment legs.

Requires JDK 21 or newer, Maven, and an existing Test Wallet address in HTTPS form (`https://ilp.interledger-test.dev/name`). The `$ilp...` display form is not a URL. No wallet key is required: the SDK's client constructor receives an ephemeral in-memory signing key, which is not used for the public lookup.

```sh
cd open-payments-java-poc
export TEST_WALLET_ADDRESS='https://ilp.interledger-test.dev/YOUR_ADDRESS'
../mvnw test exec:java
```

In another terminal: `curl http://127.0.0.1:8095/wallet`. Set `PORT` to use another free port. Stop with Ctrl+C. Only the Test Wallet host is accepted; this is not a general-purpose public proxy.

A successful lookup confirms only address discovery, not authenticated API calls, consent, or split payments.

## Authenticated grant test

In Test Wallet, open **Settings → Developer Keys**, select the account associated with your client wallet address, and generate a key pair. Record the key ID and keep the downloaded private key in a private local file outside this project. Never paste its contents into chat or commit it. See the [official setup guide](https://openpayments.dev/sdk/before-you-begin/).

Run the following from this directory after setting the values locally:

```sh
export TEST_WALLET_ADDRESS='https://ilp.interledger-test.dev/YOUR_ADDRESS'
export TEST_WALLET_KEY_ID='YOUR_KEY_ID'
export TEST_WALLET_PRIVATE_KEY_FILE='/absolute/path/to/private.key'
../mvnw compile exec:java -Dexec.mainClass=kwikpay.IncomingGrant
```

This explicitly requests an incoming-payment **create** grant against the configured wallet; the provider creates a grant and returns a token. The program prints only success or failure, does not print or persist the token, and does not create an incoming payment or transfer funds. The grant may appear in the wallet's Grants screen and can be managed there. A successful grant does not prove consent or payment execution.

## Split wallet preflight

Before configuring a split, validate the payer and both destination addresses against public Test Wallet metadata:

```sh
../mvnw compile exec:java \
  -Dexec.mainClass=kwikpay.SplitPreflight \
  -Dexec.args='https://ilp.interledger-test.dev/PAYER https://ilp.interledger-test.dev/BUSINESS https://ilp.interledger-test.dev/STOCK'
```

The preflight only accepts distinct Test Wallet URLs that resolve to ZAR with asset scale 2. It does not verify separate underlying balances, create grants or payments, or connect the application to Open Payments.

## Prepare a test split quote

`kwikpay.SplitQuote` uses the configured client wallet key to create two incoming-payment resources for ZAR 0.80 and 0.20, then requests payer-side quotes and prints the quoted debits. It is an isolated POC and does not authorize, send, track, or reconcile payments. Running it creates resources and grants in the Test Wallet environment.

Set `TEST_WALLET_ADDRESS`, `TEST_WALLET_KEY_ID`, and `TEST_WALLET_PRIVATE_KEY_FILE` as above, then run:

```sh
../mvnw compile exec:java \
  -Dexec.mainClass=kwikpay.SplitQuote \
  -Dexec.args='https://ilp.interledger-test.dev/PAYER https://ilp.interledger-test.dev/BUSINESS https://ilp.interledger-test.dev/STOCK'
```

This standalone quote command does not execute the payments. The separate SplitBridge and KwikPay API handle the interactive flow and per-leg records.
