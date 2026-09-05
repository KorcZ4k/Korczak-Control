const express = require('express');
const bcrypt = require('bcryptjs');
const User = require('../models/User');
const { requireAuth, requireRole } = require('../middleware/auth');
const { safeUser } = require('./auth');

function accountsRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  router.get('/', requireRole('FOUNDER', 'ADMINISTRATOR'), async (req, res, next) => {
    try {
      const users = await User.find().sort({ createdAt: -1 });
      res.json({ accounts: users.map(safeUser) });
    } catch (error) { next(error); }
  });

  router.post('/', requireRole('FOUNDER', 'ADMINISTRATOR'), async (req, res, next) => {
    try {
      const { name, email, password, role = 'STAFF', department = '', permissions = {}, resourcePermissions = [] } = req.body || {};
      if (typeof name !== 'string' || typeof email !== 'string' || typeof password !== 'string' || password.length < 12) {
        return res.status(400).json({ error: 'Invalid account data. Password must contain at least 12 characters.' });
      }
      if (role === 'FOUNDER') return res.status(403).json({ error: 'Founder accounts cannot be created through this route.' });
      const account = await User.create({
        name: name.trim(),
        email: email.trim().toLowerCase(),
        passwordHash: await bcrypt.hash(password, 12),
        role,
        department: typeof department === 'string' ? department.trim() : '',
        permissions,
        resourcePermissions: Array.isArray(resourcePermissions) ? resourcePermissions : []
      });
      res.status(201).json({ account: safeUser(account) });
    } catch (error) { next(error); }
  });

  router.patch('/:accountId/permissions', requireRole('FOUNDER', 'ADMINISTRATOR'), async (req, res, next) => {
    try {
      const account = await User.findOne({ accountId: req.params.accountId });
      if (!account) return res.status(404).json({ error: 'Account not found.' });
      if (account.role === 'FOUNDER' && req.auth.role !== 'FOUNDER') return res.status(403).json({ error: 'Only the founder can modify founder permissions.' });
      const { permissions, resourcePermissions, department, active, role } = req.body || {};
      if (permissions && typeof permissions === 'object') account.permissions = permissions;
      if (Array.isArray(resourcePermissions)) account.resourcePermissions = resourcePermissions;
      if (typeof department === 'string') account.department = department.trim();
      if (typeof active === 'boolean') account.active = active;
      if (typeof role === 'string' && role !== 'FOUNDER') account.role = role;
      await account.save();
      res.json({ account: safeUser(account) });
    } catch (error) { next(error); }
  });

  return router;
}

module.exports = { accountsRoutes };
