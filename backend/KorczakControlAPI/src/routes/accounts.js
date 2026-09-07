const express = require('express');
const bcrypt = require('bcryptjs');
const getUserModel = require('../models/User');
const AuditLog = require('../models/AuditLog');
const { requireAuth } = require('../middleware/auth');
const { safeUser } = require('./auth');
const { ORGANIZATION_TREE, DEPARTMENTS, roleInfo, canManageRole } = require('../config/organization');

function canManageAccount(actor, target) {
  if (!actor || !target || actor.accountId === target.accountId) return false;
  return canManageRole(actor.role, target.role);
}

async function currentActor(req) {
  return getUserModel().findById(req.auth.sub);
}

async function log(actor, target, action, details = {}) {
  await AuditLog.create({
    actorAccountId: actor.accountId,
    actorRole: actor.role,
    targetAccountId: target.accountId,
    action,
    details
  });
}

function normalizePermissions(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {};
  const permissions = { ...value };
  const mongodb = permissions.mongodb && typeof permissions.mongodb === 'object' && !Array.isArray(permissions.mongodb)
    ? { ...permissions.mongodb }
    : {};
  if (mongodb.MoonTensura !== undefined && mongodb.TensuraMoon === undefined) mongodb.TensuraMoon = Boolean(mongodb.MoonTensura);
  delete mongodb.MoonTensura;
  permissions.mongodb = mongodb;
  return permissions;
}

function validAccountData({ name, email, password }) {
  if (typeof name !== 'string' || name.trim().length < 2 || name.trim().length > 120) return 'Nome inválido.';
  if (typeof email !== 'string' || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) return 'E-mail inválido.';
  if (typeof password !== 'string' || password.length < 12) return 'A senha deve possuir pelo menos 12 caracteres.';
  return null;
}

function accountsRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  router.get('/me', async (req, res, next) => {
    try {
      const actor = await currentActor(req);
      if (!actor) return res.status(401).json({ error: 'Session unavailable.' });
      return res.json({ account: safeUser(actor), roleInfo: roleInfo(actor.role) });
    } catch (error) { return next(error); }
  });

  router.get('/organization', (req, res) => res.json({ departments: DEPARTMENTS, hierarchy: ORGANIZATION_TREE }));

  router.get('/', async (req, res, next) => {
    try {
      const User = getUserModel();
      const actor = await currentActor(req);
      if (!actor) return res.status(401).json({ error: 'Session unavailable.' });
      const users = await User.find().sort({ createdAt: -1 });
      const accounts = users.filter((user) => actor.role === 'FOUNDER' || canManageAccount(actor, user) || actor.accountId === user.accountId);
      return res.json({ accounts: accounts.map(safeUser) });
    } catch (error) { return next(error); }
  });

  router.get('/:accountId', async (req, res, next) => {
    try {
      const User = getUserModel();
      const actor = await currentActor(req);
      const account = await User.findOne({ accountId: req.params.accountId });
      if (!actor || !account) return res.status(404).json({ error: 'Account not found.' });
      if (actor.role !== 'FOUNDER' && actor.accountId !== account.accountId && !canManageAccount(actor, account)) return res.status(403).json({ error: 'You cannot view this account.' });
      const activities = (actor.role === 'FOUNDER' || canManageAccount(actor, account))
        ? await AuditLog.find({ targetAccountId: account.accountId }).sort({ createdAt: -1 }).limit(50).lean()
        : [];
      return res.json({ account: safeUser(account), roleInfo: roleInfo(account.role), activities });
    } catch (error) { return next(error); }
  });

  router.post('/', async (req, res, next) => {
    try {
      const User = getUserModel();
      const actor = await currentActor(req);
      if (!actor) return res.status(401).json({ error: 'Session unavailable.' });
      const { name, email, password, role = 'VIEWER', department = '', managerAccountId = '', permissions = {}, resourcePermissions = [] } = req.body || {};
      const validationError = validAccountData({ name, email, password });
      if (validationError) return res.status(400).json({ error: validationError });
      if (role === 'FOUNDER' || !canManageRole(actor.role, role)) return res.status(403).json({ error: 'You can only create accounts below your role.' });
      if (await User.exists({ email: email.trim().toLowerCase() })) return res.status(409).json({ error: 'Email already registered.' });

      const account = await User.create({
        name: name.trim(),
        email: email.trim().toLowerCase(),
        passwordHash: await bcrypt.hash(password, 12),
        role,
        department: typeof department === 'string' ? department.trim() : '',
        managerAccountId: typeof managerAccountId === 'string' ? managerAccountId.trim() : '',
        permissions: normalizePermissions(permissions),
        resourcePermissions: Array.isArray(resourcePermissions) ? resourcePermissions : []
      });
      await log(actor, account, 'account.created', { role, department: account.department });
      return res.status(201).json({ account: safeUser(account) });
    } catch (error) { return next(error); }
  });

  router.patch('/:accountId/permissions', async (req, res, next) => {
    try {
      const User = getUserModel();
      const actor = await currentActor(req);
      const account = await User.findOne({ accountId: req.params.accountId });
      if (!actor || !account) return res.status(404).json({ error: 'Account not found.' });
      if (actor.role !== 'FOUNDER' && !canManageAccount(actor, account)) return res.status(403).json({ error: 'You can only administer accounts below your role.' });

      const { permissions, resourcePermissions, department, active, role, managerAccountId } = req.body || {};
      if (typeof role === 'string' && role !== account.role) {
        if (role === 'FOUNDER' || !canManageRole(actor.role, role)) return res.status(403).json({ error: 'Invalid target role for your hierarchy level.' });
        account.role = role;
      }
      if (permissions && typeof permissions === 'object' && !Array.isArray(permissions)) account.permissions = normalizePermissions(permissions);
      if (Array.isArray(resourcePermissions)) account.resourcePermissions = resourcePermissions;
      if (typeof department === 'string') account.department = department.trim();
      if (typeof managerAccountId === 'string') account.managerAccountId = managerAccountId.trim();
      if (typeof active === 'boolean') account.active = active;
      account.markModified('permissions');
      await account.save();
      await log(actor, account, 'account.updated', { active: typeof active === 'boolean', role: typeof role === 'string' });
      return res.json({ account: safeUser(account) });
    } catch (error) { return next(error); }
  });

  router.patch('/:accountId/password', async (req, res, next) => {
    try {
      const User = getUserModel();
      const actor = await currentActor(req);
      const account = await User.findOne({ accountId: req.params.accountId }).select('+passwordHash');
      if (!actor || !account) return res.status(404).json({ error: 'Account not found.' });
      if (actor.role !== 'FOUNDER' && !canManageAccount(actor, account)) return res.status(403).json({ error: 'You can only reset passwords for accounts below your role.' });
      const password = req.body?.password;
      if (typeof password !== 'string' || password.length < 12) return res.status(400).json({ error: 'A nova senha deve possuir pelo menos 12 caracteres.' });
      account.passwordHash = await bcrypt.hash(password, 12);
      await account.save();
      await log(actor, account, 'account.password_reset');
      return res.json({ message: 'Password updated successfully.' });
    } catch (error) { return next(error); }
  });

  return router;
}

module.exports = { accountsRoutes };
