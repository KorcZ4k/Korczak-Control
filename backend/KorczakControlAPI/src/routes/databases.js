const express = require('express');
const { ObjectId } = require('mongodb');
const { requireAuth, requireRole } = require('../middleware/auth');
const { getDatabaseConnection, getDatabaseConnections } = require('../db');

function databasesRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));
  const requireWrite = requireRole('Owner', 'Administrator', 'Developer');

  const databaseAliases = {
    'Korczak Control': 'KorczakControl',
    KorczakControl: 'KorczakControl',
    'KZ Site': 'KorczakTechSite',
    KorczakTechSite: 'KorczakTechSite',
    Moon: 'TensuraMoon',
    TensuraMoon: 'TensuraMoon',
    TensuraBot: 'TensuraMoon'
  };

  function resolveConnection(res, requestedName) {
    const name = String(requestedName || '').trim();
    const key = databaseAliases[name] || name;
    const connection = getDatabaseConnection(key);
    if (!connection || connection.readyState !== 1 || !connection.db) {
      res.status(503).json({ error: `Database ${name || 'requested'} is not connected.` });
      return null;
    }
    return connection;
  }

  function safeCollectionName(name) {
    const value = String(name || '').trim();
    return value && !value.includes('$') && !value.startsWith('system.') && value.length <= 120 ? value : null;
  }

  async function collectionItems(db) {
    const collections = await db.listCollections({}, { nameOnly: false }).toArray();
    const items = await Promise.all(collections.map(async (collection) => {
      let estimatedDocumentCount = 0;
      try { estimatedDocumentCount = await db.collection(collection.name).estimatedDocumentCount(); } catch (_) {}
      return { name: collection.name, type: collection.type || 'collection', estimatedDocumentCount };
    }));
    return items.sort((a, b) => a.name.localeCompare(b.name));
  }

  router.get('/status', (req, res) => {
    const items = [...getDatabaseConnections().entries()].map(([key, connection]) => ({
      key, connected: connection.readyState === 1, database: connection.name || key, host: connection.host || null
    }));
    res.json({ configured: items.length > 0, items });
  });

  router.get('/', async (req, res, next) => {
    try {
      const preferred = ['KorczakControl', 'KorczakTechSite', 'TensuraMoon'];
      const items = [];
      for (const key of preferred) {
        const connection = getDatabaseConnection(key);
        if (!connection) { items.push({ name: key, key, connected: false, collections: null }); continue; }
        if (connection.readyState !== 1 || !connection.db) { items.push({ name: connection.name || key, key, connected: false, collections: null }); continue; }
        const stats = await connection.db.stats().catch(() => null);
        items.push({ name: connection.name || key, key, connected: true, collections: stats?.collections ?? null });
      }
      res.json({ items });
    } catch (error) { next(error); }
  });

  router.get('/:database/collections', async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      res.json({ database: connection.name, items: await collectionItems(connection.db) });
    } catch (error) { next(error); }
  });

  router.post('/:database/collections', requireWrite, async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      const name = safeCollectionName(req.body?.name);
      if (!name) return res.status(400).json({ error: 'Invalid collection name.' });
      const exists = await connection.db.listCollections({ name }, { nameOnly: true }).hasNext();
      if (exists) return res.status(409).json({ error: 'Collection already exists.' });
      await connection.db.createCollection(name);
      res.status(201).json({ created: true, database: connection.name, name });
    } catch (error) { next(error); }
  });

  router.get('/:database/collections/:collection/documents', async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      const collectionName = safeCollectionName(req.params.collection);
      if (!collectionName) return res.status(400).json({ error: 'Invalid collection name.' });
      const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 100);
      const skip = Math.max(Number(req.query.skip) || 0, 0);
      const collection = connection.db.collection(collectionName);
      const [items, total] = await Promise.all([collection.find({}).skip(skip).limit(limit).toArray(), collection.countDocuments()]);
      res.json({ database: connection.name, collection: collectionName, items, total, limit, skip });
    } catch (error) { next(error); }
  });

  router.post('/:database/collections/:collection/documents', requireWrite, async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      const collectionName = safeCollectionName(req.params.collection);
      const document = req.body?.document;
      if (!collectionName || !document || typeof document !== 'object' || Array.isArray(document)) return res.status(400).json({ error: 'A valid document object is required.' });
      delete document._id;
      const result = await connection.db.collection(collectionName).insertOne(document);
      res.status(201).json({ insertedId: result.insertedId, document: { ...document, _id: result.insertedId } });
    } catch (error) { next(error); }
  });

  router.patch('/:database/collections/:collection/documents/:id', requireWrite, async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      const collectionName = safeCollectionName(req.params.collection);
      const document = req.body?.document;
      if (!collectionName || !document || typeof document !== 'object' || Array.isArray(document)) return res.status(400).json({ error: 'A valid document object is required.' });
      delete document._id;
      const result = await connection.db.collection(collectionName).updateOne({ _id: new ObjectId(req.params.id) }, { $set: document });
      if (!result.matchedCount) return res.status(404).json({ error: 'Document not found.' });
      res.json({ updated: true, modified: result.modifiedCount > 0 });
    } catch (error) {
      if (error?.name === 'BSONError') return res.status(400).json({ error: 'Invalid document ID.' });
      next(error);
    }
  });

  router.delete('/:database/collections/:collection/documents/:id', requireWrite, async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      const collectionName = safeCollectionName(req.params.collection);
      if (!collectionName) return res.status(400).json({ error: 'Invalid collection name.' });
      const result = await connection.db.collection(collectionName).deleteOne({ _id: new ObjectId(req.params.id) });
      if (!result.deletedCount) return res.status(404).json({ error: 'Document not found.' });
      res.json({ deleted: true });
    } catch (error) {
      if (error?.name === 'BSONError') return res.status(400).json({ error: 'Invalid document ID.' });
      next(error);
    }
  });

  router.delete('/:database/collections/:collection', requireWrite, async (req, res, next) => {
    try {
      const connection = resolveConnection(res, req.params.database); if (!connection) return;
      const collectionName = safeCollectionName(req.params.collection);
      if (!collectionName) return res.status(400).json({ error: 'Invalid collection name.' });
      const result = await connection.db.collection(collectionName).drop().catch((error) => {
        if (error?.codeName === 'NamespaceNotFound') return false;
        throw error;
      });
      if (result === false) return res.status(404).json({ error: 'Collection not found.' });
      res.json({ deleted: true, collection: collectionName });
    } catch (error) { next(error); }
  });

  router.get('/collections', async (req, res, next) => {
    try {
      const connection = resolveConnection(res, 'KorczakControl'); if (!connection) return;
      res.json({ database: connection.name, items: await collectionItems(connection.db) });
    } catch (error) { next(error); }
  });

  return router;
}

module.exports = { databasesRoutes };
