require('dotenv').config();
const crypto = require('crypto');
const mongoose = require('mongoose');
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const { loadConfig } = require('./config');
const { connectDatabase } = require('./db');
const { authRoutes } = require('./routes/auth');
const { accountsRoutes } = require('./routes/accounts');
const { dashboardRoutes } = require('./routes/dashboard');
const { resourcesRoutes } = require('./routes/resources');
const { sitesRoutes } = require('./routes/sites');
const { githubRoutes } = require('./routes/github');
const { renderRoutes } = require('./routes/render');
const { databasesRoutes } = require('./routes/databases');
const { managedResourcesRoutes } = require('./routes/managedResources');
const { eventsRoutes } = require('./routes/events');

const config = loadConfig();
const app = express();
const authRouter = authRoutes(config);
const startedAt = Date.now();

app.disable('x-powered-by');
app.use(helmet({ contentSecurityPolicy: false }));
app.use(express.json({ limit: '256kb' }));
app.use((req, res, next) => {
  req.requestId = crypto.randomUUID();
  res.setHeader('X-Request-Id', req.requestId);
  next();
});

const allowedOrigins = config.corsOrigin.split(',').map((v) => v.trim()).filter(Boolean);
app.use(cors({
  origin(origin, cb) {
    if (!origin || allowedOrigins.length === 0 || allowedOrigins.includes(origin)) return cb(null, true);
    return cb(new Error('Origin not allowed by CORS.'));
  },
  methods: ['GET', 'POST', 'PATCH'],
  allowedHeaders: ['Content-Type', 'Authorization', 'x-bootstrap-token']
}));

function databaseConnected() {
  return mongoose.connection.readyState === 1;
}

function requireDatabase(req, res, next) {
  if (databaseConnected()) return next();
  return res.status(503).json({
    error: 'A base de dados de autenticação está indisponível.',
    requestId: req.requestId
  });
}

app.get('/', (req, res) => res.json({
  service: 'Korczak Control API',
  status: databaseConnected() ? 'online' : 'degraded',
  version: config.version
}));

app.get('/health', (req, res) => {
  const connected = databaseConnected();
  res.status(connected ? 200 : 503).json({
    status: connected ? 'ok' : 'degraded',
    service: config.serviceName,
    version: config.version,
    environment: config.environment,
    databases: {
      KorczakControl: { configured: Boolean(config.adminDbUri), connected },
      MoonTensura: { configured: Boolean(config.tensuraDbUri) },
      KorczakTechSite: { configured: Boolean(config.kzSiteDbUri) }
    },
    integrations: { github: Boolean(config.githubToken), render: Boolean(config.renderApiKey) },
    uptimeSeconds: Math.floor((Date.now() - startedAt) / 1000),
    timestamp: new Date().toISOString()
  });
});

app.use('/api/auth', requireDatabase, authRouter);
app.use('/api/accounts', requireDatabase, accountsRoutes(config));
app.use('/api/dashboard', requireDatabase, dashboardRoutes(config));
app.use('/api/resources', requireDatabase, resourcesRoutes(config));
app.use('/api/sites', requireDatabase, sitesRoutes(config));
app.use('/api/github', requireDatabase, githubRoutes(config));
app.use('/api/render', requireDatabase, renderRoutes(config));
app.use('/api/databases', requireDatabase, databasesRoutes(config));
app.use('/api/managed', requireDatabase, managedResourcesRoutes(config));
app.use('/api/events', requireDatabase, eventsRoutes(config));

app.use((req, res) => res.status(404).json({ error: 'Route not found.', requestId: req.requestId }));
app.use((error, req, res, next) => {
  console.error({ requestId: req.requestId, error: error.message });
  const status = error.statusCode || (error.name === 'MongoServerError' && error.code === 11000 ? 409 : 500);
  res.status(status).json({
    error: status === 409 ? 'Resource already exists.' : status >= 500 ? 'Internal server error.' : error.message,
    requestId: req.requestId
  });
});

let server;
async function start() {
  if (!config.adminDbUri) {
    throw new Error('ADMIN_DB_URI, KZSITE_DB_URI or MONGODB_URI is required to start the Korczak Control API.');
  }
  await connectDatabase(config.adminDbUri, config.adminDbName);
  server = app.listen(config.port, () => console.log(`Korczak Control API running on port ${config.port}`));
}

function shutdown(signal) {
  console.log(`${signal} received. Closing server.`);
  if (!server) return process.exit(0);
  server.close(async (error) => {
    try { await mongoose.disconnect(); } catch {}
    process.exit(error ? 1 : 0);
  });
}

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));
start().catch((error) => {
  console.error('Failed to start API:', error);
  process.exit(1);
});
