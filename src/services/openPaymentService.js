const pool = require('../config/database');

async function getPaymentRequests() {
    const result = await pool.query(
        `SELECT
            id,
            open_payment_id,
            wallet_address,
            amount,
            currency,
            reference,
            status,
            received_amount,
            created_at,
            expires_at
        FROM open_payment_requests
        ORDER BY created_at DESC`
    );

    return result.rows;
}

async function getPaymentRequestById(id) {
    const result = await pool.query(
        `SELECT
            id,
            open_payment_id,
            wallet_address,
            amount,
            currency,
            reference,
            status,
            received_amount,
            created_at,
            expires_at
        FROM open_payment_requests
        WHERE id = $1`,
        [id]
    );

    return result.rows[0] || null;
}

module.exports = {
    getPaymentRequests,
    getPaymentRequestById,
};