const express = require('express');
const getSiteModel = require('../models/Site');
const { requireAuth, requireRole } = require('../middleware/auth');

function sitesRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  router.get('/', async (req, res, next) => {
    try {
      const Site = getSiteModel();
      const items = await Site.find().sort({ name: 1 }).lean();
      if (items.length === 0) {
        items.push({ name: 'KZ Site', slug: 'kz-site', url: config.kzSiteApi || '', technology: 'Web', status: config.kzSiteApi ? 'operational' : 'unknown', notes: config.kzSiteApi ? 'Site configurado no ambiente.' : 'A URL operacional ainda não foi configurada.' });
      }
      res.json({ items });
    } catch (error) { next(error); }
  });

  router.get('/:slug/probe', async (req, res, next) => {
    try {
      const Site = getSiteModel();
      const item = await Site.findOne({ slug: req.params.slug }).lean();
      if (!item) return res.status(404).json({ error: 'Site not found.', requestId: req.requestId });
      const target = new URL(item.url);
      if (!['http:', 'https:'].includes(target.protocol)) return res.status(400).json({ error: 'Only HTTP(S) site URLs can be probed.', requestId: req.requestId });
      const startedAt = Date.now();
      const response = await fetch(target, { method: 'GET', redirect: 'manual', signal: AbortSignal.timeout(10000), headers: { 'User-Agent': 'Korczak-Control-Monitor/1.0' } });
      const latencyMs = Date.now() - startedAt;
      const online = response.status >= 200 && response.status < 500;
      await Site.updateOne({ _id: item._id }, { $set: { status: online ? 'operational' : 'unavailable', lastUpdatedAt: new Date() } });
      res.json({ item: { name: item.name, slug: item.slug, url: item.url }, online, statusCode: response.status, latencyMs, checkedAt: new Date().toISOString() });
    } catch (error) { if (error.name === 'TimeoutError') return res.status(504).json({ error: 'Site probe timed out.', requestId: req.requestId }); next(error); }
  });

  router.get('/:slug', async (req, res, next) => { try { const item = await getSiteModel().findOne({ slug: req.params.slug }).lean(); if (!item) return res.status(404).json({ error: 'Site not found.', requestId: req.requestId }); res.json({ item }); } catch (error) { next(error); } });
  router.post('/', requireRole('Owner', 'Administrator', 'Developer'), async (req, res, next) => {
    try {
      const { name, slug, url, repository, technology, status, notes, knownErrors } = req.body || {};
      if (!name || !slug || !url) return res.status(400).json({ error: 'name, slug and url are required.', requestId: req.requestId });
      const parsedUrl = new URL(url); if (!['http:', 'https:'].includes(parsedUrl.protocol)) return res.status(400).json({ error: 'url must use HTTP or HTTPS.', requestId: req.requestId });
      const item = await getSiteModel().create({ name, slug, url, repository, technology, status, notes, knownErrors, lastUpdatedAt: new Date() }); res.status(201).json({ item });
    } catch (error) { next(error); }
  });
  router.patch('/:slug', requireRole('Owner', 'Administrator', 'Developer'), async (req, res, next) => {
    try {
      const allowed = ['name', 'url', 'repository', 'technology', 'status', 'notes', 'knownErrors', 'lastDeploymentAt', 'lastUpdatedAt']; const update = {};
      for (const key of allowed) if (Object.prototype.hasOwnProperty.call(req.body || {}, key)) update[key] = req.body[key];
      if (update.url) { const parsedUrl = new URL(update.url); if (!['http:', 'https:'].includes(parsedUrl.protocol)) return res.status(400).json({ error: 'url must use HTTP or HTTPS.', requestId: req.requestId }); }
      update.lastUpdatedAt = new Date(); const item = await getSiteModel().findOneAndUpdate({ slug: req.params.slug }, { $set: update }, { new: true, runValidators: true });
      if (!item) return res.status(404).json({ error: 'Site not found.', requestId: req.requestId }); res.json({ item });
    } catch (error) { next(error); }
  });
  return router;
}
module.exports = { sitesRoutes };
