import 'dotenv/config';
import { createAuthenticatedClient } from '@interledger/open-payments';
import pool from '../../config/database.js';

let clientPromise;

async function getClient() {
    if (!clientPromise) {
        clientPromise = createAuthenticatedClient({
            walletAddressUrl: process.env.OPEN_PAYMENTS_CLIENT_WALLET_ADDRESS,
            privateKey: process.env.OPEN_PAYMENTS_PRIVATE_KEY_PATH,
            keyId: process.env.OPEN_PAYMENTS_KEY_ID,
            logLevel: 'debug',
        }).catch((error) => {
            // Allow a later request to retry client creation.
            clientPromise = null;
            throw error;
        });
    }

    return clientPromise;
}

function logOpenPaymentsError(stage, error) {
    console.error(`Open Payments ${stage} failed:`, {
        message: error?.message,
        description: error?.description,
        details: error?.details,
        validationErrors: error?.validationErrors,
        status: error?.status,
        code: error?.code,
    });
}

export async function createIncomingPayment({
    amount,
    currency = 'ZAR',
    reference,
}) {
    // Validate the request before calling the external service.
    const numericAmount = Number(amount);

    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
        throw new Error('Amount must be a positive number.');
    }

    if (currency !== 'ZAR') {
        throw new Error('Only ZAR is supported by this wallet.');
    }

    try {
        const client = await getClient();

        // 1. Retrieve the wallet address information.
        let wallet;

        try {
            wallet = await client.walletAddress.get({
                url: process.env.OPEN_PAYMENTS_CLIENT_WALLET_ADDRESS,
            });
        } catch (error) {
            logOpenPaymentsError('wallet lookup', error);
            throw error;
        }

        // 2. Request permission to create an incoming payment.
        let grant;

        try {
            grant = await client.grant.request(
                { url: wallet.authServer },
                {
                    access_token: {
                        access: [
                            {
                                type: 'incoming-payment',
                                actions: ['read', 'create'],
                            },
                        ],
                    },
                },
            );
        } catch (error) {
            logOpenPaymentsError('grant request', error);
            throw error;
        }

        // 3. Create the incoming payment request.
        let payment;

        try {
            payment = await client.incomingPayment.create(
                {
                    url: wallet.resourceServer,
                    accessToken: grant.access_token.value,
                },
                {
                    walletAddress: wallet.id,
                    incomingAmount: {
                        assetCode: wallet.assetCode,
                        assetScale: wallet.assetScale,
                        value: String(
                            Math.round(
                                numericAmount * 10 ** wallet.assetScale,
                            ),
                        ),
                    },
                    metadata: {
                        description: reference || 'Spaza shop purchase',
                    },
                },
            );
        } catch (error) {
            logOpenPaymentsError('incoming payment creation', error);
            throw error;
        }

        await pool.query(
            `INSERT INTO open_payment_requests (
            open_payment_id,
            wallet_address,
            amount,
            currency,
            reference,
            status,
            received_amount,
            expires_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
            [
                payment.id,
                wallet.id,
                numericAmount,
                currency,
                reference || 'Spaza shop purchase',
                payment.completed ? 'COMPLETED' : 'PENDING',
                Number(payment.receivedAmount?.value ?? 0) /
                10 ** wallet.assetScale,
                payment.expiresAt ?? null,
            ],
        );

        return payment;
    } catch (error) {
        // Let the controller return an HTTP error response.
        // Do not terminate the server process.
        throw error;
    }
}
