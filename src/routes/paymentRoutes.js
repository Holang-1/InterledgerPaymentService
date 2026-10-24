const express = require('express');
const paymentController = require('../controllers/paymentController');

const router = express.Router();

router.post('/', paymentController.createPayment);
router.get('/', paymentController.getAllPayments);
router.get('/:id', paymentController.getPaymentById);
router.delete('/:id', paymentController.deletePayment);

module.exports = router;