const express = require('express');
const getUserModel = require('../models/User');
const getAuditLogModel = require('../models/AuditLog');
const { requireAuth } = require('../middleware/auth');
const { canManageRole } = require('../config/organization');

function sanitizeLimit(value, fallback = 100) {
  const parsed = Number.parseInt(String(value || ''), 10);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.max(1, Math.min(parsed, 250));
}

function auditRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  router.get('/', async (req, res, next) => {
    try {
      const User = getUserModel();
      const AuditLog = getAuditLogModel();
      const actor = await User.findById(req.auth.sub).lean();
      if (!actor || !actor.active) return res.status(401).json({ error: 'Session unavailable.' });

      const accountId = String(req.query.accountId || '').trim();
      const action = String(req.query.action || '').trim();
      const search = String(req.query.search || '').trim();
      const limit = sanitizeLimit(req.query.limit);
      const before = req.query.before ? new Date(String(req.query.before)) : null;

      let allowedTargets = null;
      if (actor.role !== 'FOUNDER') {
        const users = await User.find({}, { accountId: 1, role: 1 }).lean();
        allowedTargets = new Set(users
          .filter((user) => user.accountId === actor.accountId || canManageRole(actor.role, user.role))
          .map((user) => user.accountId));
      }

      if (accountId && allowedTargets && !allowedTargets.has(accountId)) {
        return res.status(403).json({ error: 'You cannot view this account audit.' });
      }

      const query = {};
      if (accountId) query.targetAccountId = accountId;
      if (action) query.action = action;
      if (before && !Number.isNaN(before.getTime())) query.createdAt = { $lt: before };
      if (allowedTargets) query.targetAccountId = accountId || { $in: [...allowedTargets] };

      const rawActivities = await AuditLog.find(query).sort({ createdAt: -1 }).limit(limit).lean();
      const visibleActivities = search
        ? rawActivities.filter((item) => `${item.action} ${item.actorAccountId} ${item.targetAccountId}`.toLowerCase().includes(search.toLowerCase()))
        : rawActivities;

      const ids = [...new Set(visibleActivities.flatMap((item) => [item.actorAccountId, item.targetAccountId]).filter(Boolean))];
      const users = ids.length ? await User.find({ accountId: { $in: ids } }, { accountId: 1, name: 1, email: 1 }).lean() : [];
      const people = new Map(users.map((user) => [user.accountId, user]));

      const activities = visibleActivities.map((item) => ({
        id: item._id.toString(),
        action: item.action,
        actorAccountId: item.actorAccountId,
        actorRole: item.actorRole,
        targetAccountId: item.targetAccountId,
        actorName: people.get(item.actorAccountId)?.name || item.actorAccountId || 'Sistema',
        targetName: people.get(item.targetAccountId)?.name || item.targetAccountId || 'Sem conta associada',
        details: item.details || {},
        createdAt: item.createdAt
      }));

      const summary = activities.reduce((result, item) => {
        result.total += 1;
        result.byAction[item.action] = (result.byAction[item.action] || 0) + 1;
        return result;
      }, { total: 0, byAction: {} });

      return res.json({ activities, summary, nextBefore: activities.at(-1)?.createdAt || null });
    } catch (error) {
      return next(error);
    }
  });

  return router;
}

module.exports = { auditRoutes };
