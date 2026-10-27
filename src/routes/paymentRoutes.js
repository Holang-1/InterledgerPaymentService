const openPaymentsController = require('../controllers/openPaymentsController');
const express = require('express');
const paymentController = require('../controllers/paymentController');

const router = express.Router();

router.post('/', paymentController.createPayment);
router.post('/open-payments', openPaymentsController.createIncomingPayment);
router.get('/open-payments', openPaymentsController.getPaymentRequests);
router.get('/open-payments/:id', openPaymentsController.getPaymentRequestById);
router.get('/', paymentController.getAllPayments);
router.get('/:id', paymentController.getPaymentById);
router.delete('/:id', paymentController.deletePayment);

module.exports = router;