async function createIncomingPayment(req, res) {
    try {
        const { amount, currency = 'ZAR', reference } = req.body;

        const { createIncomingPayment: createPayment } =
            await import('../integrations/open-payments/incomingPayment.mjs');

        const payment = await createPayment({
            amount,
            currency,
            reference,
        });

        return res.status(201).json({
            message: 'Incoming payment request created.',
            payment,
        });
    } catch (error) {
        console.error('Open Payments error:', error.message);

        return res.status(400).json({
            error: error.message,
        });
    }
}

const openPaymentService = require('../services/openPaymentService');

async function getPaymentRequests(req, res) {
    try {
        const payments = await openPaymentService.getPaymentRequests();
        return res.status(200).json({ payments });
    } catch (error) {
        console.error('Failed to retrieve payment requests:', error.message);
        return res.status(500).json({
            error: 'Failed to retrieve payment requests.',
        });
    }
}

async function getPaymentRequestById(req, res) {
    try {
        const { id } = req.params;

        const payment = await openPaymentService.getPaymentRequestById(id);

        if (!payment) {
            return res.status(404).json({
                error: 'Payment request not found.',
            });
        }

        return res.status(200).json({ payment });
    } catch (error) {
        console.error('Failed to retrieve payment request:', error.message);
        return res.status(500).json({
            error: 'Failed to retrieve payment request.',
        });
    }
}

module.exports = {
    createIncomingPayment,
    getPaymentRequests,
    getPaymentRequestById,
};