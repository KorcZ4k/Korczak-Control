const express = require('express');
const { requireAuth, requireRole } = require('../middleware/auth');
const { getDatabaseConnection } = require('../db');

const aliases = { 'Korczak Control': 'KorczakControl', KorczakControl: 'KorczakControl', 'KZ Site': 'KorczakTechSite', KorczakTechSite: 'KorczakTechSite', Moon: 'TensuraMoon', TensuraMoon: 'TensuraMoon', TensuraBot: 'TensuraMoon' };
function safeName(value) { const name = String(value || '').trim(); return name && !name.includes('$') && !name.startsWith('system.') && name.length <= 120 ? name : null; }
function databaseExtrasRoutes(config) {
  const router = express.Router(); router.use(requireAuth(config)); const write = requireRole('Owner', 'Administrator', 'Developer');
  function resolve(name) { const connection = getDatabaseConnection(aliases[name] || name); if (!connection || connection.readyState !== 1 || !connection.db) { const error = new Error('Database connection is unavailable.'); error.statusCode = 503; throw error; } return connection; }
  router.patch('/:database/collections/:collection', write, async (req, res, next) => { try { const connection = resolve(req.params.database); const from = safeName(req.params.collection); const to = safeName(req.body?.name); if (!from || !to) return res.status(400).json({ error: 'Valid source and destination collection names are required.' }); if (from === to) return res.json({ renamed: false, name: from }); const exists = await connection.db.listCollections({ name: to }, { nameOnly: true }).hasNext(); if (exists) return res.status(409).json({ error: 'Destination collection already exists.' }); await connection.db.collection(from).rename(to); res.json({ renamed: true, from, to }); } catch (error) { next(error); } });
  router.get('/:database/collections/:collection/search', async (req, res, next) => { try { const connection = resolve(req.params.database); const collection = safeName(req.params.collection); const q = String(req.query.q || '').trim(); if (!collection || !q) return res.status(400).json({ error: 'Collection and search query are required.' }); const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 100); const sample = await connection.db.collection(collection).find({}).limit(1).toArray(); const fields = sample[0] ? Object.keys(sample[0]).filter((key) => key !== '_id') : []; const regex = { $regex: q, $options: 'i' }; const filter = fields.length ? { $or: fields.map((field) => ({ [field]: regex })) } : {}; const items = await connection.db.collection(collection).find(filter).limit(limit).toArray(); res.json({ database: connection.name, collection, query: q, items, total: items.length }); } catch (error) { next(error); } });
  return router;
}
module.exports = { databaseExtrasRoutes };
