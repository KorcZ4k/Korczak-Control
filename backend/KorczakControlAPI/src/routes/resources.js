const express = require('express');
const { requireAuth } = require('../middleware/auth');

const groups = ['sites', 'apis', 'apps', 'databases', 'infrastructure'];

function resourcesRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  for (const group of groups) {
    router.get(`/${group}`, (req, res) => {
      res.json({ group, items: [], implemented: false });
    });
  }

  return router;
}

module.exports = { resourcesRoutes };
