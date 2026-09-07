const express = require('express');
const getApplicationModel = require('../models/Application');
const { requireAuth, requireRole } = require('../middleware/auth');

function defaults(config) { return [
  { name: 'Korczak Control', slug: 'korczak-control', status: 'operational', version: config.version || '', platforms: ['Android', 'Desktop'], repository: 'KorcZ4k/Korczak-Control', apiUrl: config.kzControlApi || '', databaseKey: 'KorczakControl', notes: 'Aplicativo administrativo principal.' },
  { name: 'Tensura Moon', slug: 'tensura-moon', status: 'unknown', platforms: ['Bot'], repository: config.tensuraMoonGithubRepo || '', databaseKey: 'TensuraMoon', botSlug: 'tensura-moon', notes: 'Projeto e bot Tensura Moon.' },
  { name: 'KZ Site', slug: 'kz-site', status: config.kzSiteApi ? 'operational' : 'unknown', platforms: ['Web'], apiUrl: config.kzSiteApi || '', databaseKey: 'KorczakTechSite', notes: 'Aplicação web da Korczak Technologies.' }
]; }

function applicationsRoutes(config) {
  const router = express.Router(); router.use(requireAuth(config));
  const write = requireRole('Owner', 'Administrator', 'Developer'); const Application = () => getApplicationModel();
  router.get('/', async (req, res, next) => { try { const items = await Application().find().sort({ name: 1 }).lean(); res.json({ items: items.length ? items : defaults(config), source: items.length ? 'database' : 'defaults' }); } catch (error) { next(error); } });
  router.get('/:slug', async (req, res, next) => { try { const item = await Application().findOne({ slug: req.params.slug }).lean(); if (item) return res.json({ item }); const fallback = defaults(config).find((entry) => entry.slug === req.params.slug); if (!fallback) return res.status(404).json({ error: 'Application not found.' }); res.json({ item: fallback, source: 'defaults' }); } catch (error) { next(error); } });
  router.post('/', write, async (req, res, next) => { try { const body = req.body || {}; if (!body.name || !body.slug) return res.status(400).json({ error: 'name and slug are required.' }); const item = await Application().create({ ...body, history: [{ event: 'Application registered', details: `Registered by ${req.user?.username || req.user?.name || 'authorized user'}` }] }); res.status(201).json({ item }); } catch (error) { next(error); } });
  router.patch('/:slug', write, async (req, res, next) => { try { const allowed = ['name','status','version','platforms','repository','apiUrl','siteUrl','databaseKey','botSlug','lastUpdatedAt','notes']; const update = {}; for (const key of allowed) if (Object.prototype.hasOwnProperty.call(req.body || {}, key)) update[key] = req.body[key]; update.lastUpdatedAt = new Date(); const item = await Application().findOneAndUpdate({ slug: req.params.slug }, { $set: update, $push: { history: { event: 'Application updated', details: `Updated by ${req.user?.username || req.user?.name || 'authorized user'}` } } }, { new: true, runValidators: true }); if (!item) return res.status(404).json({ error: 'Application not found.' }); res.json({ item }); } catch (error) { next(error); } });
  router.delete('/:slug', write, async (req, res, next) => { try { const item = await Application().findOneAndDelete({ slug: req.params.slug }); if (!item) return res.status(404).json({ error: 'Application not found.' }); res.json({ deleted: true }); } catch (error) { next(error); } });
  return router;
}
module.exports = { applicationsRoutes };
