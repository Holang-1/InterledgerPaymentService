const pool = require('../config/database');

const VALID_CURRENCIES = ['ZAR', 'USD', 'EUR'];

const UUID_PATTERN =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

async function createPayment(paymentData) {
    const {
        payerId,
        payeeId,
        amount,
        currency,
        paymentReference = null
    } = paymentData;

    if (!payerId || !payeeId) {
        throw new Error('Payer ID and payee ID are required.');
    }

    if (payerId === payeeId) {
        throw new Error('Payer and payee must be different.');
    }

    const parsedAmount = Number(amount);

    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
        throw new Error('Amount must be a positive number.');
    }

    if (!VALID_CURRENCIES.includes(currency)) {
        throw new Error('Unsupported currency.');
    }

    const query = `
INSERT INTO payments (
    payer_id,
    payee_id,
    amount,
    currency,
    payment_reference
)
VALUES ($1, $2, $3, $4, $5)
RETURNING
id,
    payer_id AS "payerId",
    payee_id AS "payeeId",
    amount,
    currency,
    status,
    payment_reference AS "paymentReference",
    created_at AS "createdAt",
    updated_at AS "updatedAt"
    `;

    const values = [
        payerId,
        payeeId,
        parsedAmount,
        currency,
        paymentReference
    ];

    const result = await pool.query(query, values);

    return result.rows[0];
}

async function getAllPayments() {
    const query = `
SELECT
id,
    payer_id AS "payerId",
    payee_id AS "payeeId",
    amount,
    currency,
    status,
    payment_reference AS "paymentReference",
    created_at AS "createdAt",
    updated_at AS "updatedAt"
FROM payments
ORDER BY created_at DESC
    `;

    const result = await pool.query(query);

    return result.rows;
}

async function getPaymentById(id) {
    if (typeof id !== 'string' || !UUID_PATTERN.test(id)) {
        return null;
    }

    const query = `
SELECT
id,
    payer_id AS "payerId",
    payee_id AS "payeeId",
    amount,
    currency,
    status,
    payment_reference AS "paymentReference",
    created_at AS "createdAt",
    updated_at AS "updatedAt"
FROM payments
WHERE id = $1
    `;

    const result = await pool.query(query, [id]);

    return result.rows[0] || null;
}

async function deletePayment(id) {
    if (typeof id !== 'string' || !UUID_PATTERN.test(id)) {
        return false;
    }

    const query = `
        DELETE FROM payments
        WHERE id = $1
        RETURNING id
    `;

    const result = await pool.query(query, [id]);

    return result.rowCount > 0;
}

module.exports = {
    createPayment,
    getAllPayments,
    getPaymentById,
    deletePayment
};