const express = require('express');
const mongoose = require('mongoose');
const Site = require('../models/Site');
const ManagedResource = require('../models/ManagedResource');
const ControlEvent = require('../models/ControlEvent');
const { requireAuth, requireRole } = require('../middleware/auth');

function emptyServices() { return { online: 0, attention: 0, unavailable: 0, unknown: 0 }; }
function mapStatus(status, services) {
  if (status === 'operational') services.online += 1;
  else if (status === 'attention' || status === 'maintenance') services.attention += 1;
  else if (status === 'unavailable') services.unavailable += 1;
  else services.unknown += 1;
}

function dashboardRoutes(config) {
  const router = express.Router();

  router.get('/summary', requireAuth(config), async (req, res, next) => {
    try {
      const connected = mongoose.connection.readyState === 1;
      const [sites, apiResources, appResources, unreadEvents] = connected
        ? await Promise.all([
            Site.find({}, { status: 1 }).lean(),
            ManagedResource.find({ kind: 'api' }, { status: 1 }).lean(),
            ManagedResource.find({ kind: 'app' }, { status: 1 }).lean(),
            ControlEvent.countDocuments({ readBy: { $ne: String(req.auth.sub) } })
          ])
        : [[], [], [], 0];

      const services = emptyServices();
      [...sites, ...apiResources, ...appResources].forEach((item) => mapStatus(item.status, services));

      res.json({
        generatedAt: new Date().toISOString(),
        user: { id: req.auth.sub, role: req.auth.role },
        resources: {
          sites: sites.length,
          apis: apiResources.length,
          apps: appResources.length,
          databases: connected ? 1 : 0,
          infrastructure: 2
        },
        services,
        notifications: { unread: unreadEvents },
        integrations: {
          github: Boolean(config.githubToken),
          render: Boolean(config.renderApiKey),
          mongodb: connected
        }
      });
    } catch (error) { next(error); }
  });

  router.get('/admin-check', requireAuth(config), requireRole('Owner', 'Administrator'), (req, res) => {
    res.json({ allowed: true, role: req.auth.role });
  });

  return router;
}

module.exports = { dashboardRoutes };
