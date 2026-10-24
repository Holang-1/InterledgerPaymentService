require('dotenv').config();

const app = require('./app');

const PORT = process.env.PORT || 3000;

const server = app.listen(PORT, () => {
    console.log(`InterledgerPaymentService running on port ${PORT}`);
});

// Handle unexpected server errors
server.on('error', (error) => {
    console.error('Server failed to start:', error.message);
    process.exitCode = 1;
});
