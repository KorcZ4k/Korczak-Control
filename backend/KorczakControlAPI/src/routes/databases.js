const express = require('express');
const mongoose = require('mongoose');
const { requireAuth } = require('../middleware/auth');

function databasesRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));
  router.get('/status', (req, res) => {
    const connection = mongoose.connection;
    res.json({ configured: Boolean(config.mongoUri), connected: connection.readyState === 1, database: connection.name || null, host: connection.host || null });
  });
  router.get('/collections', async (req, res, next) => {
    try {
      if (mongoose.connection.readyState !== 1) return res.status(503).json({ error: 'MongoDB is not connected.', requestId: req.requestId });
      const collections = await mongoose.connection.db.listCollections().toArray();
      const items = await Promise.all(collections.map(async (collection) => {
        const name = collection.name;
        const count = await mongoose.connection.db.collection(name).estimatedDocumentCount();
        return { name, type: collection.type, estimatedDocumentCount: count };
      }));
      res.json({ database: mongoose.connection.name, items: items.sort((a, b) => a.name.localeCompare(b.name)) });
    } catch (error) { next(error); }
  });
  router.get('/stats', async (req, res, next) => {
    try {
      if (mongoose.connection.readyState !== 1) return res.status(503).json({ error: 'MongoDB is not connected.', requestId: req.requestId });
      const stats = await mongoose.connection.db.stats();
      res.json({ database: mongoose.connection.name, collections: stats.collections, objects: stats.objects, dataSize: stats.dataSize, storageSize: stats.storageSize, indexes: stats.indexes, indexSize: stats.indexSize });
    } catch (error) { next(error); }
  });
  return router;
}
module.exports = { databasesRoutes };
