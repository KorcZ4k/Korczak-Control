const express = require('express');
const { requireAuth, requireRole } = require('../middleware/auth');
const { getDatabase, getDatabaseConnection, getDatabaseConnections } = require('../db');

const RESOURCE_COLLECTIONS = [
  ['Users', 'users'],
  ['Customers', 'customers'],
  ['Applications', 'applications'],
  ['Bots', 'bots'],
  ['Sites', 'sites']
];

function adminDb() {
  const db = getDatabase('KorczakControl');
  if (!db) {
    const error = new Error('KorczakControl database is unavailable.');
    error.statusCode = 503;
    throw error;
  }
  return db;
}

function safeText(value) {
  return String(value || '').trim().toLowerCase();
}

async function count(db, collection) {
  try { return await db.collection(collection).countDocuments(); }
  catch { return 0; }
}

function controlCenterRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  // Global search across administrative resources. Results are normalized so clients never need to render raw MongoDB objects.
  router.get('/search', async (req, res, next) => {
    try {
      const query = safeText(req.query.q);
      const limit = Math.min(Math.max(Number(req.query.limit) || 20, 1), 100);
      if (query.length < 2) return res.json({ query, total: 0, results: [] });
      const db = adminDb();
      const regex = new RegExp(query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i');
      const results = [];
      for (const [collection, type] of RESOURCE_COLLECTIONS) {
        const exists = await db.listCollections({ name: collection }).hasNext();
        if (!exists) continue;
        const documents = await db.collection(collection).find({
          $or: [
            { name: regex }, { username: regex }, { email: regex },
            { id: regex }, { customerId: regex }, { _id: regex }
          ]
        }).limit(limit).toArray();
        for (const item of documents) {
          results.push({
            type,
            id: String(item._id || item.id || item.customerId || ''),
            title: item.name || item.username || item.email || item.customerId || 'Sem nome',
            subtitle: item.role || item.status || item.email || item.project || '',
            status: item.status || null
          });
        }
      }
      res.json({ query, total: results.length, results: results.slice(0, limit) });
    } catch (error) { next(error); }
  });

  // Consolidated statistics used by Android and Desktop.
  router.get('/statistics', async (req, res, next) => {
    try {
      const db = adminDb();
      const [users, customers, applications, bots, sites, auditEvents] = await Promise.all([
        count(db, 'Users'), count(db, 'Customers'), count(db, 'Applications'),
        count(db, 'Bots'), count(db, 'Sites'), count(db, 'AuditLogs')
      ]);
      const activeUsers = await db.collection('Users').countDocuments({ status: { $ne: 'disabled' } }).catch(() => 0);
      const activeCustomers = await db.collection('Customers').countDocuments({ status: { $ne: 'inactive' } }).catch(() => 0);
      res.json({
        generatedAt: new Date().toISOString(),
        accounts: { total: users, active: activeUsers },
        customers: { total: customers, active: activeCustomers },
        resources: { applications, bots, sites },
        activity: { auditEvents },
        databases: [...getDatabaseConnections().entries()].map(([key, connection]) => ({ key, connected: connection.readyState === 1, name: connection.name }))
      });
    } catch (error) { next(error); }
  });

  // Read-only backup manifest. Actual destructive restore operations are intentionally isolated and owner-only.
  router.get('/backup/manifest', requireRole('Administrator', 'Owner'), async (req, res, next) => {
    try {
      const databases = ['KorczakControl', 'KorczakTechSite', 'TensuraMoon'];
      const manifest = [];
      for (const key of databases) {
        const connection = getDatabaseConnection(key);
        if (!connection || connection.readyState !== 1) {
          manifest.push({ key, available: false, collections: [] });
          continue;
        }
        const db = getDatabase(key);
        const collections = await db.listCollections({}, { nameOnly: true }).toArray();
        const details = [];
        for (const collection of collections) details.push({ name: collection.name, documents: await db.collection(collection.name).countDocuments() });
        manifest.push({ key, available: true, collections: details });
      }
      res.json({ generatedAt: new Date().toISOString(), manifest });
    } catch (error) { next(error); }
  });

  // Monitoring summary intentionally uses health data already known to the API; external checks can be added without blocking the UI.
  router.get('/monitoring', async (req, res) => {
    const services = [
      { name: 'Korczak Control API', status: 'online' },
      ...['KorczakControl', 'KorczakTechSite', 'TensuraMoon'].map((key) => {
        const connection = getDatabaseConnection(key);
        return { name: `MongoDB ${key}`, status: connection?.readyState === 1 ? 'online' : 'offline' };
      }),
      { name: 'GitHub', status: config.githubToken ? 'configured' : 'not_configured' },
      { name: 'Render', status: config.renderApiKey ? 'configured' : 'not_configured' }
    ];
    res.json({ generatedAt: new Date().toISOString(), services });
  });

  router.get('/report', async (req, res, next) => {
    try {
      const db = adminDb();
      const [statistics, recentAudit] = await Promise.all([
        Promise.all(RESOURCE_COLLECTIONS.map(async ([collection, type]) => ({ type, total: await count(db, collection) }))),
        db.collection('AuditLogs').find({}).sort({ createdAt: -1, timestamp: -1 }).limit(20).toArray().catch(() => [])
      ]);
      res.json({ generatedAt: new Date().toISOString(), statistics, recentActivity: recentAudit.map((item) => ({ id: String(item._id), action: item.action || item.event || 'Ação registrada', module: item.module || 'Sistema', createdAt: item.createdAt || item.timestamp || null })) });
    } catch (error) { next(error); }
  });

  return router;
}

module.exports = { controlCenterRoutes };
