const express = require('express');
const ManagedResource = require('../models/ManagedResource');
const { requireAuth, requireRole } = require('../middleware/auth');

function managedResourcesRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  router.get('/:kind', async (req, res, next) => {
    try {
      const { kind } = req.params;
      if (!['api', 'app'].includes(kind)) return res.status(404).json({ error: 'Resource type not found.', requestId: req.requestId });
      res.json({ items: await ManagedResource.find({ kind }).sort({ name: 1 }).lean() });
    } catch (error) { next(error); }
  });

  router.get('/:kind/:slug', async (req, res, next) => {
    try {
      const item = await ManagedResource.findOne({ kind: req.params.kind, slug: req.params.slug }).lean();
      if (!item) return res.status(404).json({ error: 'Resource not found.', requestId: req.requestId });
      res.json({ item });
    } catch (error) { next(error); }
  });

  router.post('/:kind', requireRole('Owner', 'Administrator', 'Developer'), async (req, res, next) => {
    try {
      const { kind } = req.params;
      if (!['api', 'app'].includes(kind)) return res.status(400).json({ error: 'Invalid resource type.', requestId: req.requestId });
      const { name, slug, url, repository, technology, version, status, latencyMs, notes } = req.body || {};
      if (!name || !slug) return res.status(400).json({ error: 'name and slug are required.', requestId: req.requestId });
      const item = await ManagedResource.create({ name, slug, kind, url, repository, technology, version, status, latencyMs, notes, lastActivityAt: new Date() });
      res.status(201).json({ item });
    } catch (error) { next(error); }
  });

  router.patch('/:kind/:slug', requireRole('Owner', 'Administrator', 'Developer'), async (req, res, next) => {
    try {
      const allowed = ['name', 'url', 'repository', 'technology', 'version', 'status', 'latencyMs', 'lastActivityAt', 'notes'];
      const update = {};
      for (const key of allowed) if (Object.prototype.hasOwnProperty.call(req.body || {}, key)) update[key] = req.body[key];
      const item = await ManagedResource.findOneAndUpdate({ kind: req.params.kind, slug: req.params.slug }, { $set: update }, { new: true, runValidators: true });
      if (!item) return res.status(404).json({ error: 'Resource not found.', requestId: req.requestId });
      res.json({ item });
    } catch (error) { next(error); }
  });

  return router;
}
module.exports = { managedResourcesRoutes };
