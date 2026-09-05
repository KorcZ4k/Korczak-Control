require('dotenv').config();

const express = require('express');
const helmet = require('helmet');
const cors = require('cors');

const app = express();
const PORT = Number(process.env.PORT || 3000);
const startedAt = new Date();

app.disable('x-powered-by');
app.use(helmet({ contentSecurityPolicy: false }));
app.use(express.json({ limit: '256kb' }));

const allowedOrigin = process.env.CORS_ORIGIN;
app.use(cors({
  origin: allowedOrigin ? allowedOrigin : false,
  methods: ['GET'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

app.get('/', (req, res) => {
  res.json({
    service: 'Korczak Control API',
    status: 'online',
    version: '0.1.0'
  });
});

app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'ok',
    service: 'korczak-control-api',
    version: '0.1.0',
    environment: process.env.NODE_ENV || 'development',
    uptimeSeconds: Math.floor((Date.now() - startedAt.getTime()) / 1000),
    timestamp: new Date().toISOString()
  });
});

app.use((req, res) => {
  res.status(404).json({
    error: 'Route not found.'
  });
});

app.use((error, req, res, next) => {
  console.error(error);
  res.status(500).json({
    error: 'Internal server error.'
  });
});

app.listen(PORT, () => {
  console.log(`Korczak Control API running on port ${PORT}`);
});
