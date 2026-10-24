const paymentService = require('../services/paymentService');

async function createPayment(req, res) {
    try {
        const payment = await paymentService.createPayment(req.body);

        return res.status(201).json({
            message: 'Payment record created.',
            payment
        });
    } catch (error) {
        if (
            error.message.includes('required') ||
            error.message.includes('different') ||
            error.message.includes('positive') ||
            error.message.includes('Unsupported')
        ) {
            return res.status(400).json({
                error: error.message
            });
        }

        if (error.code === '23505') {
            return res.status(409).json({
                error: 'That payment reference already exists.'
            });
        }

        console.error('Failed to create payment:', error.message);

        return res.status(500).json({
            error: 'Failed to create payment record.'
        });
    }
}

async function getAllPayments(req, res) {
    try {
        const payments = await paymentService.getAllPayments();

        return res.status(200).json({ payments });
    } catch (error) {
        console.error('Failed to retrieve payments:', error.message);

        return res.status(500).json({
            error: 'Failed to retrieve payments.'
        });
    }
}

async function getPaymentById(req, res) {
    try {
        const payment = await paymentService.getPaymentById(req.params.id);

        if (!payment) {
            return res.status(404).json({
                error: 'Payment not found.'
            });
        }

        return res.status(200).json({ payment });
    } catch (error) {
        console.error('Failed to retrieve payment:', error.message);

        return res.status(500).json({
            error: 'Failed to retrieve payment.'
        });
    }
}
async function deletePayment(req, res) {
    try {
        const deleted = await paymentService.deletePayment(req.params.id);

        if (!deleted) {
            return res.status(404).json({
                error: 'Payment not found.'
            });
        }

        return res.status(200).json({
            message: 'Payment deleted successfully.'
        });
    } catch (error) {
        console.error('Failed to delete payment:', error.message);

        return res.status(500).json({
            error: 'Failed to delete payment.'
        });
    }
}


module.exports = {
    createPayment,
    getAllPayments,
    getPaymentById,
    deletePayment
};