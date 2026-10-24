const express = require('express');
const cors = require('cors');
const pool = require('./config/database');
const paymentRoutes = require('./routes/paymentRoutes');

const app = express();

// Middleware
app.use(cors());
app.use(express.json());
app.use('/api/payments', paymentRoutes);

// Basic health check
app.get('/health', (req, res) => {
    res.status(200).json({
        status: 'UP',
        service: 'InterledgerPaymentService'
    });
});

// Database health check
app.get('/health/database', async (req, res) => {
    try {
        const result = await pool.query('SELECT NOW() AS database_time');

        res.status(200).json({
            status: 'UP',
            database: 'connected',
            time: result.rows[0].database_time
        });
    } catch (error) {
        console.error('Database health check failed:', error.message);

        res.status(503).json({
            status: 'DOWN',
            database: 'unavailable'
        });
    }
});

module.exports = app;