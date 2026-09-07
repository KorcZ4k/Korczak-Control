const express = require('express');
const getCustomerModel = require('../models/Customer');
const { requireAuth, requireRole } = require('../middleware/auth');

function customersRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));
  const write = requireRole('Owner', 'Administrator', 'Developer', 'Manager', 'Employee');
  const admin = requireRole('Owner', 'Administrator', 'Developer', 'Manager');
  const Customer = () => getCustomerModel();

  router.get('/', async (req, res, next) => {
    try {
      const search = String(req.query.search || '').trim();
      const status = String(req.query.status || '').trim();
      const filter = {};
      if (status) filter.status = status;
      if (search) filter.$or = ['name', 'externalId', 'email', 'phone', 'service'].map((field) => ({ [field]: { $regex: search, $options: 'i' } }));
      const items = await Customer().find(filter).sort({ updatedAt: -1 }).lean();
      res.json({ items, total: items.length });
    } catch (error) { next(error); }
  });

  router.get('/:id', async (req, res, next) => { try { const item = await Customer().findById(req.params.id).lean(); if (!item) return res.status(404).json({ error: 'Client not found.' }); res.json({ item }); } catch (error) { next(error); } });

  router.post('/', write, async (req, res, next) => {
    try {
      const { name, externalId, email, phone, status, service, registeredAt, notes } = req.body || {};
      if (!String(name || '').trim() || !String(externalId || '').trim()) return res.status(400).json({ error: 'name and externalId are required.' });
      const item = await Customer().create({ name, externalId, email, phone, status, service, registeredAt, notes, history: [{ action: 'Client created', details: `Created by ${req.user?.username || req.user?.name || 'authorized user'}` }] });
      res.status(201).json({ item });
    } catch (error) { next(error); }
  });

  router.patch('/:id', write, async (req, res, next) => {
    try {
      const allowed = ['name', 'externalId', 'email', 'phone', 'status', 'service', 'registeredAt', 'notes']; const update = {};
      for (const key of allowed) if (Object.prototype.hasOwnProperty.call(req.body || {}, key)) update[key] = req.body[key];
      const item = await Customer().findByIdAndUpdate(req.params.id, { $set: update, $push: { history: { action: 'Client updated', details: `Updated by ${req.user?.username || req.user?.name || 'authorized user'}` } } }, { new: true, runValidators: true });
      if (!item) return res.status(404).json({ error: 'Client not found.' }); res.json({ item });
    } catch (error) { next(error); }
  });

  router.delete('/:id', admin, async (req, res, next) => { try { const item = await Customer().findByIdAndDelete(req.params.id); if (!item) return res.status(404).json({ error: 'Client not found.' }); res.json({ deleted: true }); } catch (error) { next(error); } });
  return router;
}
module.exports = { customersRoutes };
