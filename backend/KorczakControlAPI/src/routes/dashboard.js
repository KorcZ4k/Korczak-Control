const express = require('express');
const { requireAuth, requireRole } = require('../middleware/auth');

function dashboardRoutes(config) {
  const router = express.Router();

  router.get('/summary', requireAuth(config), async (req, res) => {
    res.json({
      generatedAt: new Date().toISOString(),
      user: { id: req.auth.sub, role: req.auth.role },
      resources: {
        sites: 0,
        apis: 0,
        apps: 0,
        databases: 0,
        infrastructure: 0
      },
      services: { online: 0, attention: 0, unavailable: 0, unknown: 0 },
      integrations: {
        github: Boolean(config.githubToken),
        render: Boolean(config.renderApiKey),
        mongodb: Boolean(config.mongoUri)
      }
    });
  });

  router.get('/admin-check', requireAuth(config), requireRole('Owner', 'Administrator'), (req, res) => {
    res.json({ allowed: true, role: req.auth.role });
  });

  return router;
}

module.exports = { dashboardRoutes };
