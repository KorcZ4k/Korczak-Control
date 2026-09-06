const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const User = require('../models/User');
const { requireAuth } = require('../middleware/auth');

const founderPermissions = {
  github: true,
  render: true,
  mongodb: { KorczakControl: true, MoonTensura: true, KorczakTechSite: true },
  bots: true,
  sites: true,
  applications: true,
  apis: true
};

function normalizedPermissions(user) {
  const permissions = user.permissions?.toObject ? user.permissions.toObject() : { ...(user.permissions || {}) };
  const mongodb = { ...(permissions.mongodb || {}) };
  if (mongodb.Admin && !mongodb.KorczakControl) mongodb.KorczakControl = true;
  delete mongodb.Admin;
  return { ...permissions, mongodb };
}

function safeUser(user) {
  return {
    id: user._id.toString(),
    accountId: user.accountId,
    name: user.name,
    email: user.email,
    role: user.role,
    department: user.department,
    permissions: normalizedPermissions(user),
    resourcePermissions: user.resourcePermissions,
    active: user.active
  };
}

function normalizeEmail(value) {
  return typeof value === 'string' ? value.trim().toLowerCase() : '';
}

function validEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

async function migrateLegacyPermissions(user) {
  if (user.permissions?.mongodb?.Admin && !user.permissions.mongodb.KorczakControl) {
    user.permissions.mongodb.KorczakControl = true;
    user.permissions.mongodb.Admin = false;
    user.markModified('permissions');
    await user.save();
  }
}

async function countUsers() {
  return User.countDocuments();
}

function authRoutes(config) {
  const router = express.Router();

  router.get('/bootstrap-status', async (req, res, next) => {
    try {
      const accountCount = await countUsers();
      return res.json({ setupRequired: accountCount === 0 });
    } catch (error) { next(error); }
  });

  router.post('/register', async (req, res, next) => {
    try {
      const existingCount = await countUsers();
      const isFounder = existingCount === 0;

      if (!isFounder && config.environment === 'production') {
        return res.status(403).json({ error: 'Public registration is disabled.' });
      }

      const { name, email, password } = req.body || {};
      const normalizedEmail = normalizeEmail(email);

      if (typeof name !== 'string' || typeof password !== 'string' || name.trim().length < 2 || password.length < 12) {
        return res.status(400).json({ error: 'Invalid registration data. Password must contain at least 12 characters.' });
      }
      if (!validEmail(normalizedEmail)) return res.status(400).json({ error: 'Invalid email.' });

      const existingUser = await User.findOne({ email: normalizedEmail }).collation({ locale: 'en', strength: 2 });
      if (existingUser) return res.status(409).json({ error: 'Email already registered.' });

      const user = await User.create({
        name: name.trim(),
        email: normalizedEmail,
        passwordHash: await bcrypt.hash(password, 12),
        role: isFounder ? 'FOUNDER' : 'VIEWER',
        department: isFounder ? 'Korczak Technologies' : '',
        permissions: isFounder ? founderPermissions : {}
      });

      return res.status(201).json({ user: safeUser(user), setupCompleted: isFounder });
    } catch (error) { next(error); }
  });

  router.post('/login', async (req, res, next) => {
    try {
      const { email, password } = req.body || {};
      const normalizedEmail = normalizeEmail(email);

      if (typeof password !== 'string' || !validEmail(normalizedEmail) || password.length === 0) {
        return res.status(400).json({ error: 'Informe um e-mail e uma senha válidos.' });
      }

      const accountCount = await countUsers();
      if (accountCount === 0) {
        return res.status(409).json({ error: 'Nenhuma conta foi configurada ainda. Crie a primeira conta para continuar.', code: 'SETUP_REQUIRED' });
      }

      const user = await User.findOne({ email: normalizedEmail })
        .collation({ locale: 'en', strength: 2 })
        .select('+passwordHash');

      if (!user || !user.active || typeof user.passwordHash !== 'string' || user.passwordHash.length === 0) {
        return res.status(401).json({ error: 'E-mail ou senha incorretos.' });
      }

      let passwordMatches = false;
      try {
        passwordMatches = await bcrypt.compare(password, user.passwordHash);
      } catch {
        passwordMatches = false;
      }
      if (!passwordMatches) return res.status(401).json({ error: 'E-mail ou senha incorretos.' });

      await migrateLegacyPermissions(user);
      user.lastLoginAt = new Date();
      await user.save();

      const token = jwt.sign(
        { sub: user._id.toString(), accountId: user.accountId, role: user.role },
        config.jwtSecret,
        { expiresIn: '8h', issuer: 'korczak-control-api' }
      );

      return res.json({ token, user: safeUser(user) });
    } catch (error) { next(error); }
  });

  router.get('/me', requireAuth(config), async (req, res, next) => {
    try {
      const user = await User.findById(req.auth.sub);
      if (!user || !user.active) return res.status(401).json({ error: 'Session unavailable.' });
      await migrateLegacyPermissions(user);
      return res.json({ user: safeUser(user) });
    } catch (error) { next(error); }
  });

  return router;
}

module.exports = { authRoutes, founderPermissions, safeUser };