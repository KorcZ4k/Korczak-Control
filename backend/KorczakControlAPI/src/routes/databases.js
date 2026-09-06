const express = require('express');
const { requireAuth, requireRole } = require('../middleware/auth');
const { getDatabaseConnection, getDatabaseConnections } = require('../db');

function databasesRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));
  const requireWrite = requireRole('Owner', 'Administrator', 'Developer');

  const databaseAliases = {
    KorczakControl: 'KorczakControl',
    KorczakTechSite: 'KorczakTechSite',
    TensuraMoon: 'TensuraMoon'
  };

  function resolveConnection(res, requestedName) {
    const name = String(requestedName || '').trim();
    const key = databaseAliases[name] || name;
    const connection = getDatabaseConnection(key);
    if (!connection || connection.readyState !== 1) {
      res.status(503).json({ error: `Database ${name || 'requested'} is not connected.` });
      return null;
    }
    return connection;
  }

  async function collectionItems(db) {
    const collections = await db.listCollections().toArray();
    return Promise.all(collections.map(async (collection) => ({
      name: collection.name,
      type: collection.type,
      estimatedDocumentCount: await db.collection(collection.name).estimatedDocumentCount()
    })));
  }

  router.get('/status', (req, res) => {
    const items = [...getDatabaseConnections().entries()].map(([key, connection]) => ({
      key,
      connected: connection.readyState === 1,
      database: connection.name || null,
      host: connection.host || null
    }));
    res.json({ configured: items.length > 0, items });
  });

  router.get('/databases', async (req, res, next) => {
    try {
      const items = [];
      for (const [key, connection] of getDatabaseConnections().entries()) {
        if (connection.readyState !== 1) continue;
        const stats = await connection.db.stats().catch(() => null);
        items.push({
          name: connection.name || key,
          key,
          connected: true,
          collections: stats?.collections ?? null
        });
      }
      res.json({ items: items.sort((a, b) => a.name.localeCompare(b.name)) });
    } catch (error) { next(error); }
  });

  router.get('/databases/:database/collections', async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      const items = await collectionItems(connection.db);
      res.json({ database: connection.name, items: items.sort((a, b) => a.name.localeCompare(b.name)) });
    } catch (error) { next(error); }
  });

  router.post('/databases/:database/collections', requireWrite, async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      const name = String(req.body?.name || '').trim();
      if (!name || name.includes('$') || name.startsWith('system.')) {
        return res.status(400).json({ error: 'Invalid collection name.' });
      }
      await connection.db.createCollection(name);
      res.status(201).json({ created: true, database: connection.name, name });
    } catch (error) { next(error); }
  });

  router.get('/databases/:database/collections/:collection/documents', async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 100);
      const skip = Math.max(Number(req.query.skip) || 0, 0);
      const collection = connection.db.collection(req.params.collection);
      const [items, total] = await Promise.all([
        collection.find({}).skip(skip).limit(limit).toArray(),
        collection.countDocuments()
      ]);
      res.json({ database: connection.name, collection: req.params.collection, items, total, limit, skip });
    } catch (error) { next(error); }
  });

  router.post('/databases/:database/collections/:collection/documents', requireWrite, async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      const document = req.body?.document;
      if (!document || typeof document !== 'object' || Array.isArray(document)) {
        return res.status(400).json({ error: 'A document object is required.' });
      }
      const result = await connection.db.collection(req.params.collection).insertOne(document);
      res.status(201).json({ insertedId: result.insertedId, document });
    } catch (error) { next(error); }
  });

  // Compatibility routes for KorczakControl.
  router.get('/collections', async (req, res, next) => {
    try {
      const connection = resolveConnection(res, 'KorczakControl'); if (!connection) return;
      const items = await collectionItems(connection.db);
      res.json({ database: connection.name, items: items.sort((a, b) => a.name.localeCompare(b.name)) });
    } catch (error) { next(error); }
  });

  router.get('/collections/:collection/documents', async (req, res, next) => {
    try {
      const connection = resolveConnection(res, 'KorczakControl'); if (!connection) return;
      const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 100);
      const skip = Math.max(Number(req.query.skip) || 0, 0);
      const collection = connection.db.collection(req.params.collection);
      const [items, total] = await Promise.all([
        collection.find({}).skip(skip).limit(limit).toArray(),
        collection.countDocuments()
      ]);
      res.json({ database: connection.name, collection: req.params.collection, items, total, limit, skip });
    } catch (error) { next(error); }
  });

  router.post('/collections', requireWrite, async (req, res, next) => {
    try {
      const connection = resolveConnection(res, 'KorczakControl'); if (!connection) return;
      const name = String(req.body?.name || '').trim();
      if (!name || name.includes('$') || name.startsWith('system.')) {
        return res.status(400).json({ error: 'Invalid collection name.' });
      }
      await connection.db.createCollection(name);
      res.status(201).json({ created: true, database: connection.name, name });
    } catch (error) { next(error); }
  });

  router.get('/stats', async (req, res, next) => {
    try {
      const connection = resolveConnection(res, 'KorczakControl'); if (!connection) return;
      const stats = await connection.db.stats();
      res.json({
        database: connection.name,
        collections: stats.collections,
        objects: stats.objects,
        dataSize: stats.dataSize,
        storageSize: stats.storageSize,
        indexes: stats.indexes,
        indexSize: stats.indexSize
      });
    } catch (error) { next(error); }
  });

  return router;
}

module.exports = { databasesRoutes };
