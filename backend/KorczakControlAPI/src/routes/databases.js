const express = require('express');
const mongoose = require('mongoose');
const { requireAuth, requireRole } = require('../middleware/auth');

function databasesRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));
  const requireWrite = requireRole('Owner', 'Administrator', 'Developer');

  function clientOrFail(res) {
    if (mongoose.connection.readyState !== 1 || !mongoose.connection.getClient) {
      res.status(503).json({ error: 'MongoDB is not connected.' });
      return null;
    }
    return mongoose.connection.getClient();
  }

  function databaseOrFail(res, name) {
    const client = clientOrFail(res);
    if (!client) return null;
    const databaseName = String(name || '').trim();
    if (!databaseName || databaseName.includes('\u0000')) {
      res.status(400).json({ error: 'Invalid database name.' });
      return null;
    }
    return client.db(databaseName);
  }

  function currentDbOrFail(res) {
    const client = clientOrFail(res);
    if (!client) return null;
    return client.db(mongoose.connection.name);
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
    const connection = mongoose.connection;
    res.json({
      configured: Boolean(config.adminDbUri),
      connected: connection.readyState === 1,
      database: connection.name || null,
      host: connection.host || null
    });
  });

  // Lists every database visible to the configured MongoDB user.
  router.get('/databases', async (req, res, next) => {
    try {
      const client = clientOrFail(res); if (!client) return;
      const result = await client.db('admin').admin().listDatabases({ nameOnly: false });
      const items = (result.databases || []).map((database) => ({
        name: database.name,
        sizeOnDisk: database.sizeOnDisk,
        empty: database.empty
      })).sort((a, b) => a.name.localeCompare(b.name));
      res.json({ items });
    } catch (error) { next(error); }
  });

  router.get('/databases/:database/collections', async (req, res, next) => {
    try {
      const db = databaseOrFail(res, req.params.database); if (!db) return;
      const items = await collectionItems(db);
      res.json({ database: db.databaseName, items: items.sort((a, b) => a.name.localeCompare(b.name)) });
    } catch (error) { next(error); }
  });

  router.post('/databases/:database/collections', requireWrite, async (req, res, next) => {
    try {
      const db = databaseOrFail(res, req.params.database); if (!db) return;
      const name = String(req.body?.name || '').trim();
      if (!name || name.includes('$') || name.startsWith('system.')) return res.status(400).json({ error: 'Invalid collection name.' });
      await db.createCollection(name);
      res.status(201).json({ created: true, database: db.databaseName, name });
    } catch (error) { next(error); }
  });

  router.get('/databases/:database/collections/:collection/documents', async (req, res, next) => {
    try {
      const db = databaseOrFail(res, req.params.database); if (!db) return;
      const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 100);
      const skip = Math.max(Number(req.query.skip) || 0, 0);
      const collection = db.collection(req.params.collection);
      const [items, total] = await Promise.all([
        collection.find({}).skip(skip).limit(limit).toArray(),
        collection.countDocuments()
      ]);
      res.json({ database: db.databaseName, collection: req.params.collection, items, total, limit, skip });
    } catch (error) { next(error); }
  });

  router.post('/databases/:database/collections/:collection/documents', requireWrite, async (req, res, next) => {
    try {
      const db = databaseOrFail(res, req.params.database); if (!db) return;
      const document = req.body?.document;
      if (!document || typeof document !== 'object' || Array.isArray(document)) return res.status(400).json({ error: 'document object is required.' });
      const result = await db.collection(req.params.collection).insertOne(document);
      res.status(201).json({ insertedId: result.insertedId, document });
    } catch (error) { next(error); }
  });

  // Backward-compatible routes for the default KorczakControl database.
  router.get('/collections', async (req, res, next) => {
    try {
      const db = currentDbOrFail(res); if (!db) return;
      const items = await collectionItems(db);
      res.json({ database: db.databaseName, items: items.sort((a, b) => a.name.localeCompare(b.name)) });
    } catch (error) { next(error); }
  });

  router.get('/collections/:collection/documents', async (req, res, next) => {
    try {
      const db = currentDbOrFail(res); if (!db) return;
      const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 100);
      const skip = Math.max(Number(req.query.skip) || 0, 0);
      const collection = db.collection(req.params.collection);
      const [items, total] = await Promise.all([collection.find({}).skip(skip).limit(limit).toArray(), collection.countDocuments()]);
      res.json({ database: db.databaseName, collection: req.params.collection, items, total, limit, skip });
    } catch (error) { next(error); }
  });

  router.post('/collections', requireWrite, async (req, res, next) => {
    try {
      const db = currentDbOrFail(res); if (!db) return;
      const name = String(req.body?.name || '').trim();
      if (!name || name.includes('$') || name.startsWith('system.')) return res.status(400).json({ error: 'Invalid collection name.' });
      await db.createCollection(name);
      res.status(201).json({ created: true, database: db.databaseName, name });
    } catch (error) { next(error); }
  });

  router.get('/stats', async (req, res, next) => {
    try {
      const db = currentDbOrFail(res); if (!db) return;
      const stats = await db.stats();
      res.json({ database: db.databaseName, collections: stats.collections, objects: stats.objects, dataSize: stats.dataSize, storageSize: stats.storageSize, indexes: stats.indexes, indexSize: stats.indexSize });
    } catch (error) { next(error); }
  });

  return router;
}
module.exports = { databasesRoutes };
