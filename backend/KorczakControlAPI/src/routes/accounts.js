const express = require('express');
const bcrypt = require('bcryptjs');
const User = require('../models/User');
const AuditLog = require('../models/AuditLog');
const { requireAuth } = require('../middleware/auth');
const { safeUser } = require('./auth');
const { ORGANIZATION_TREE, DEPARTMENTS, roleInfo, canManageRole } = require('../config/organization');

function canManageAccount(actor, target) {
  if (!actor || !target) return false;
  if (actor.accountId === target.accountId) return false;
  return canManageRole(actor.role, target.role);
}
async function currentActor(req) { return User.findById(req.auth.sub); }
async function log(actor, target, action, details = {}) { await AuditLog.create({ actorAccountId: actor.accountId, actorRole: actor.role, targetAccountId: target.accountId, action, details }); }

function accountsRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  router.get('/me', async (req, res, next) => {
    try {
      const actor = await currentActor(req);
      if (!actor) return res.status(401).json({ error: 'Session unavailable.' });
      res.json({ account: safeUser(actor), roleInfo: roleInfo(actor.role) });
    } catch (error) { next(error); }
  });

  router.get('/organization', (req, res) => res.json({ departments: DEPARTMENTS, hierarchy: ORGANIZATION_TREE }));
  router.get('/', async (req, res, next) => {
    try {
      const actor = await currentActor(req);
      if (!actor) return res.status(401).json({ error: 'Session unavailable.' });
      const users = await User.find().sort({ createdAt: -1 });
      const accounts = users.filter((user) => actor.role === 'FOUNDER' || canManageAccount(actor, user) || actor.accountId === user.accountId);
      res.json({ accounts: accounts.map(safeUser) });
    } catch (error) { next(error); }
  });

  router.get('/:accountId', async (req, res, next) => {
    try {
      const actor = await currentActor(req); const account = await User.findOne({ accountId: req.params.accountId });
      if (!actor || !account) return res.status(404).json({ error: 'Account not found.' });
      if (actor.role !== 'FOUNDER' && actor.accountId !== account.accountId && !canManageAccount(actor, account)) return res.status(403).json({ error: 'You cannot view this account.' });
      const activities = (actor.role === 'FOUNDER' || canManageAccount(actor, account)) ? await AuditLog.find({ targetAccountId: account.accountId }).sort({ createdAt: -1 }).limit(50).lean() : [];
      res.json({ account: safeUser(account), roleInfo: roleInfo(account.role), activities });
    } catch (error) { next(error); }
  });

  router.post('/', async (req, res, next) => {
    try {
      const actor = await currentActor(req);
      if (!actor) return res.status(401).json({ error: 'Session unavailable.' });
      const { name, email, password, role = 'VIEWER', department = '', managerAccountId = '', permissions = {}, resourcePermissions = [] } = req.body || {};
      if (typeof name !== 'string' || typeof email !== 'string' || typeof password !== 'string' || password.length < 12) return res.status(400).json({ error: 'Invalid account data. Password must contain at least 12 characters.' });
      if (role === 'FOUNDER' || !canManageRole(actor.role, role)) return res.status(403).json({ error: 'You can only create accounts below your role.' });
      const account = await User.create({ name: name.trim(), email: email.trim().toLowerCase(), passwordHash: await bcrypt.hash(password, 12), role, department: department.trim(), managerAccountId, permissions, resourcePermissions });
      await log(actor, account, 'account.created', { role, department: account.department });
      res.status(201).json({ account: safeUser(account) });
    } catch (error) { next(error); }
  });

  router.patch('/:accountId/permissions', async (req, res, next) => {
    try {
      const actor = await currentActor(req); const account = await User.findOne({ accountId: req.params.accountId });
      if (!actor || !account) return res.status(404).json({ error: 'Account not found.' });
      if (actor.role !== 'FOUNDER' && !canManageAccount(actor, account)) return res.status(403).json({ error: 'You can only administer accounts below your role.' });
      const { permissions, resourcePermissions, department, active, role, managerAccountId } = req.body || {};
      if (typeof role === 'string' && role !== account.role) { if (role === 'FOUNDER' || !canManageRole(actor.role, role)) return res.status(403).json({ error: 'Invalid target role for your hierarchy level.' }); account.role = role; }
      if (permissions && typeof permissions === 'object') account.permissions = permissions;
      if (Array.isArray(resourcePermissions)) account.resourcePermissions = resourcePermissions;
      if (typeof department === 'string') account.department = department.trim();
      if (typeof managerAccountId === 'string') account.managerAccountId = managerAccountId.trim();
      if (typeof active === 'boolean') account.active = active;
      account.markModified('permissions'); await account.save();
      await log(actor, account, 'account.permissions_updated', { changed: { permissions: Boolean(permissions), resourcePermissions: Array.isArray(resourcePermissions), department: typeof department === 'string', active: typeof active === 'boolean', role: typeof role === 'string', managerAccountId: typeof managerAccountId === 'string' } });
      res.json({ account: safeUser(account) });
    } catch (error) { next(error); }
  });
  return router;
}
module.exports = { accountsRoutes };
