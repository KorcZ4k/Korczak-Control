const express = require('express');
const Site = require('../models/Site');
const { requireAuth, requireRole } = require('../middleware/auth');

function sitesRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  router.get('/', async (req, res, next) => {
    try { res.json({ items: await Site.find().sort({ name: 1 }).lean() }); } catch (error) { next(error); }
  });

  router.get('/:slug', async (req, res, next) => {
    try {
      const item = await Site.findOne({ slug: req.params.slug }).lean();
      if (!item) return res.status(404).json({ error: 'Site not found.', requestId: req.requestId });
      res.json({ item });
    } catch (error) { next(error); }
  });

  router.post('/', requireRole('Owner', 'Administrator', 'Developer'), async (req, res, next) => {
    try {
      const { name, slug, url, repository, technology, status, notes, knownErrors } = req.body || {};
      if (!name || !slug || !url) return res.status(400).json({ error: 'name, slug and url are required.', requestId: req.requestId });
      const item = await Site.create({ name, slug, url, repository, technology, status, notes, knownErrors, lastUpdatedAt: new Date() });
      res.status(201).json({ item });
    } catch (error) { next(error); }
  });

  router.patch('/:slug', requireRole('Owner', 'Administrator', 'Developer'), async (req, res, next) => {
    try {
      const allowed = ['name', 'url', 'repository', 'technology', 'status', 'notes', 'knownErrors', 'lastDeploymentAt', 'lastUpdatedAt'];
      const update = {};
      for (const key of allowed) if (Object.prototype.hasOwnProperty.call(req.body || {}, key)) update[key] = req.body[key];
      update.lastUpdatedAt = new Date();
      const item = await Site.findOneAndUpdate({ slug: req.params.slug }, { $set: update }, { new: true, runValidators: true });
      if (!item) return res.status(404).json({ error: 'Site not found.', requestId: req.requestId });
      res.json({ item });
    } catch (error) { next(error); }
  });

  return router;
}

module.exports = { sitesRoutes };
