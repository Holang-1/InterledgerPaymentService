import 'dotenv/config';
import { createAuthenticatedClient } from '@interledger/open-payments';

async function main() {
    const client = await createAuthenticatedClient({
        walletAddressUrl: process.env.OPEN_PAYMENTS_CLIENT_WALLET_ADDRESS,
        privateKey: process.env.OPEN_PAYMENTS_PRIVATE_KEY_PATH,
        keyId: process.env.OPEN_PAYMENTS_KEY_ID,
    });

    const wallet = await client.walletAddress.get({
        url: process.env.OPEN_PAYMENTS_CLIENT_WALLET_ADDRESS,
    });

    console.log('Open Payments connection successful!');
    console.log('Wallet address:', wallet.id);
    console.log('Currency:', wallet.assetCode);
}

main().catch((error) => {
    console.error('Open Payments connection failed.');
    console.error(error.message);
    process.exitCode = 1;
});