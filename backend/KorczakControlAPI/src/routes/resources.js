const express = require('express');
const mongoose = require('mongoose');
const Site = require('../models/Site');
const ManagedResource = require('../models/ManagedResource');
const { requireAuth } = require('../middleware/auth');

function resourcesRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  router.get('/sites', async (req, res, next) => {
    try { res.json({ group: 'sites', items: await Site.find().sort({ name: 1 }).lean(), implemented: true }); }
    catch (error) { next(error); }
  });

  for (const [group, kind] of [['apis', 'api'], ['apps', 'app']]) {
    router.get(`/${group}`, async (req, res, next) => {
      try { res.json({ group, items: await ManagedResource.find({ kind }).sort({ name: 1 }).lean(), implemented: true }); }
      catch (error) { next(error); }
    });
  }

  router.get('/databases', (req, res) => {
    const connected = mongoose.connection.readyState === 1;
    res.json({ group: 'databases', implemented: true, items: connected ? [{ name: mongoose.connection.name, status: 'connected', host: mongoose.connection.host }] : [] });
  });

  router.get('/infrastructure', (req, res) => {
    res.json({ group: 'infrastructure', implemented: true, items: [
      { name: 'Korczak Control API', status: 'operational' },
      { name: 'MongoDB', status: mongoose.connection.readyState === 1 ? 'operational' : 'unknown' },
      { name: 'GitHub', status: config.githubToken ? 'configured' : 'not-configured' },
      { name: 'Render', status: config.renderApiKey ? 'configured' : 'not-configured' }
    ] });
  });

  return router;
}

module.exports = { resourcesRoutes };
