const express = require('express');
const ControlEvent = require('../models/ControlEvent');
const { requireAuth, requireRole } = require('../middleware/auth');

function eventsRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  async function loadItems(req) {
    const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 200);
    const filter = {};
    if (req.query.category) filter.category = req.query.category;
    if (req.query.severity) filter.severity = req.query.severity;
    return ControlEvent.find(filter).sort({ createdAt: -1 }).limit(limit).lean();
  }

  router.get('/', async (req, res, next) => {
    try { res.json({ items: await loadItems(req) }); } catch (error) { next(error); }
  });

  // Alias used by older Android builds and the dashboard. Keeping it avoids Route not found during staged deployments.
  router.get('/list', async (req, res, next) => {
    try { res.json({ items: await loadItems(req) }); } catch (error) { next(error); }
  });

  router.get('/summary', async (req, res, next) => {
    try {
      const actorId = String(req.auth?.sub || '');
      const [total, unread, items] = await Promise.all([
        ControlEvent.countDocuments(),
        ControlEvent.countDocuments({ readBy: { $ne: actorId } }),
        ControlEvent.find({}).sort({ createdAt: -1 }).limit(10).lean()
      ]);
      res.json({ total, unread, items });
    } catch (error) { next(error); }
  });

  router.get('/unread', async (req, res, next) => {
    try {
      const actorId = String(req.auth?.sub || '');
      const items = await ControlEvent.find({ readBy: { $ne: actorId } }).sort({ createdAt: -1 }).limit(100).lean();
      res.json({ items, count: items.length });
    } catch (error) { next(error); }
  });

  router.post('/', requireRole('Owner', 'Administrator', 'Developer'), async (req, res, next) => {
    try {
      const { category, severity, title, message, resourceType, resourceId, metadata } = req.body || {};
      if (!title) return res.status(400).json({ error: 'title is required.', requestId: req.requestId });
      const item = await ControlEvent.create({ category, severity, title, message, resourceType, resourceId, metadata, actorId: String(req.auth.sub) });
      res.status(201).json({ item });
    } catch (error) { next(error); }
  });

  router.post('/:id/read', async (req, res, next) => {
    try {
      const item = await ControlEvent.findByIdAndUpdate(req.params.id, { $addToSet: { readBy: String(req.auth.sub) } }, { new: true });
      if (!item) return res.status(404).json({ error: 'Event not found.', requestId: req.requestId });
      res.json({ item });
    } catch (error) { next(error); }
  });

  return router;
}
module.exports = { eventsRoutes };