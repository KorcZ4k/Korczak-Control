require('dotenv').config();

const crypto = require('crypto');
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const { loadConfig } = require('./config');

const config = loadConfig();
const app = express();
const startedAt = Date.now();

app.disable('x-powered-by');
app.use(helmet({ contentSecurityPolicy: false }));
app.use(express.json({ limit: '256kb' }));

app.use((req, res, next) => {
  req.requestId = crypto.randomUUID();
  res.setHeader('X-Request-Id', req.requestId);
  next();
});

const allowedOrigins = config.corsOrigin
  .split(',')
  .map((origin) => origin.trim())
  .filter(Boolean);

app.use(cors({
  origin(origin, callback) {
    if (!origin || allowedOrigins.length === 0 || allowedOrigins.includes(origin)) {
      return callback(null, true);
    }
    return callback(new Error('Origin not allowed by CORS.'));
  },
  methods: ['GET'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

app.get('/', (req, res) => {
  res.json({
    service: 'Korczak Control API',
    status: 'online',
    version: config.version
  });
});

app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'ok',
    service: config.serviceName,
    version: config.version,
    environment: config.environment,
    uptimeSeconds: Math.floor((Date.now() - startedAt) / 1000),
    timestamp: new Date().toISOString()
  });
});

app.use((req, res) => {
  res.status(404).json({
    error: 'Route not found.',
    requestId: req.requestId
  });
});

app.use((error, req, res, next) => {
  console.error({ requestId: req.requestId, error: error.message });
  res.status(500).json({
    error: 'Internal server error.',
    requestId: req.requestId
  });
});

const server = app.listen(config.port, () => {
  console.log(`Korczak Control API running on port ${config.port}`);
});

function shutdown(signal) {
  console.log(`${signal} received. Closing server.`);
  server.close((error) => {
    if (error) {
      console.error(error);
      process.exit(1);
    }
    process.exit(0);
  });
}

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));
