const express = require('express');
const mongoose = require('mongoose');
const { requireAuth, requireRole } = require('../middleware/auth');

function databasesRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));
  const requireWrite = requireRole('Owner', 'Administrator', 'Developer');

  function dbOrFail(res) {
    if (mongoose.connection.readyState !== 1) {
      res.status(503).json({ error: 'MongoDB is not connected.' });
      return null;
    }
    return mongoose.connection.db;
  }

  router.get('/status', (req, res) => {
    const connection = mongoose.connection;
    res.json({ configured: Boolean(config.adminDbUri), connected: connection.readyState === 1, database: connection.name || null, host: connection.host || null });
  });
  router.get('/collections', async (req, res, next) => {
    try {
      const db = dbOrFail(res); if (!db) return;
      const collections = await db.listCollections().toArray();
      const items = await Promise.all(collections.map(async (collection) => ({ name: collection.name, type: collection.type, estimatedDocumentCount: await db.collection(collection.name).estimatedDocumentCount() })));
      res.json({ database: db.databaseName, items: items.sort((a, b) => a.name.localeCompare(b.name)) });
    } catch (error) { next(error); }
  });
  router.get('/collections/:collection/documents', async (req, res, next) => {
    try {
      const db = dbOrFail(res); if (!db) return;
      const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 100);
      const skip = Math.max(Number(req.query.skip) || 0, 0);
      const collection = db.collection(req.params.collection);
      const [items, total] = await Promise.all([collection.find({}).skip(skip).limit(limit).toArray(), collection.countDocuments()]);
      res.json({ collection: req.params.collection, items, total, limit, skip });
    } catch (error) { next(error); }
  });
  router.post('/collections', requireWrite, async (req, res, next) => {
    try {
      const db = dbOrFail(res); if (!db) return;
      const name = String(req.body?.name || '').trim();
      if (!name || name.includes('$') || name.startsWith('system.')) return res.status(400).json({ error: 'Invalid collection name.' });
      await db.createCollection(name);
      res.status(201).json({ created: true, name });
    } catch (error) { next(error); }
  });
  router.post('/collections/:collection/documents', requireWrite, async (req, res, next) => {
    try {
      const db = dbOrFail(res); if (!db) return;
      const document = req.body?.document;
      if (!document || typeof document !== 'object' || Array.isArray(document)) return res.status(400).json({ error: 'document object is required.' });
      const result = await db.collection(req.params.collection).insertOne(document);
      res.status(201).json({ insertedId: result.insertedId, document });
    } catch (error) { next(error); }
  });
  router.patch('/collections/:collection/documents/:id', requireWrite, async (req, res, next) => {
    try {
      const db = dbOrFail(res); if (!db) return;
      const { ObjectId } = mongoose.mongo;
      if (!ObjectId.isValid(req.params.id)) return res.status(400).json({ error: 'Invalid document id.' });
      const update = req.body?.update;
      if (!update || typeof update !== 'object' || Array.isArray(update)) return res.status(400).json({ error: 'update object is required.' });
      const result = await db.collection(req.params.collection).findOneAndUpdate({ _id: new ObjectId(req.params.id) }, { $set: update }, { returnDocument: 'after' });
      if (!result) return res.status(404).json({ error: 'Document not found.' });
      res.json({ item: result });
    } catch (error) { next(error); }
  });
  router.delete('/collections/:collection/documents/:id', requireWrite, async (req, res, next) => {
    try {
      const db = dbOrFail(res); if (!db) return;
      const { ObjectId } = mongoose.mongo;
      if (!ObjectId.isValid(req.params.id)) return res.status(400).json({ error: 'Invalid document id.' });
      const result = await db.collection(req.params.collection).deleteOne({ _id: new ObjectId(req.params.id) });
      if (!result.deletedCount) return res.status(404).json({ error: 'Document not found.' });
      res.json({ deleted: true });
    } catch (error) { next(error); }
  });
  router.delete('/collections/:collection', requireWrite, async (req, res, next) => {
    try {
      const db = dbOrFail(res); if (!db) return;
      if (req.params.collection.startsWith('system.')) return res.status(400).json({ error: 'System collections cannot be removed.' });
      await db.collection(req.params.collection).drop();
      res.json({ deleted: true });
    } catch (error) { next(error); }
  });
  router.get('/stats', async (req, res, next) => {
    try {
      const db = dbOrFail(res); if (!db) return;
      const stats = await db.stats();
      res.json({ database: db.databaseName, collections: stats.collections, objects: stats.objects, dataSize: stats.dataSize, storageSize: stats.storageSize, indexes: stats.indexes, indexSize: stats.indexSize });
    } catch (error) { next(error); }
  });
  return router;
}
module.exports = { databasesRoutes };
