const express = require('express');
const getBotModel = require('../models/Bot');
const { requireAuth, requireRole } = require('../middleware/auth');
const { githubRequest } = require('../integrations/github/client');

function configuredTensura(config) {
  const value = String(config.tensuraMoonGithubRepo || '').trim().replace(/^https?:\/\/github\.com\//, '').replace(/\.git$/, '');
  return value || '';
}
function defaults(config) { return [{ name: 'Tensura Moon', slug: 'tensura-moon', project: 'Tensura Moon', status: 'unknown', repository: configuredTensura(config), databaseKey: 'TensuraMoon', notes: 'Bot vinculado exclusivamente ao projeto Tensura Moon.' }]; }

function botsRoutes(config) {
  const router = express.Router(); router.use(requireAuth(config));
  const write = requireRole('Owner', 'Administrator', 'Developer'); const Bot = () => getBotModel();
  router.get('/', async (req, res, next) => { try { const items = await Bot().find().sort({ project: 1, name: 1 }).lean(); res.json({ items: items.length ? items : defaults(config), source: items.length ? 'database' : 'defaults' }); } catch (error) { next(error); } });
  router.get('/:slug', async (req, res, next) => { try { const item = await Bot().findOne({ slug: req.params.slug }).lean(); if (item) return res.json({ item }); const fallback = defaults(config).find((entry) => entry.slug === req.params.slug); if (!fallback) return res.status(404).json({ error: 'Bot not found.' }); res.json({ item: fallback, source: 'defaults' }); } catch (error) { next(error); } });
  router.post('/', write, async (req, res, next) => { try { const body = req.body || {}; if (!body.name || !body.slug || !body.project) return res.status(400).json({ error: 'name, slug and project are required.' }); const item = await Bot().create(body); res.status(201).json({ item }); } catch (error) { next(error); } });
  router.patch('/:slug', write, async (req, res, next) => { try { const allowed = ['name','project','status','version','repository','databaseKey','lastUpdatedAt','lastError','notes']; const update = {}; for (const key of allowed) if (Object.prototype.hasOwnProperty.call(req.body || {}, key)) update[key] = req.body[key]; const item = await Bot().findOneAndUpdate({ slug: req.params.slug }, { $set: update }, { new: true, runValidators: true }); if (!item) return res.status(404).json({ error: 'Bot not found.' }); res.json({ item }); } catch (error) { next(error); } });
  router.delete('/:slug', write, async (req, res, next) => { try { const item = await Bot().findOneAndDelete({ slug: req.params.slug }); if (!item) return res.status(404).json({ error: 'Bot not found.' }); res.json({ deleted: true }); } catch (error) { next(error); } });
  router.get('/:slug/workflows', async (req, res, next) => { try { const item = await Bot().findOne({ slug: req.params.slug }).lean() || defaults(config).find((entry) => entry.slug === req.params.slug); if (!item?.repository) return res.status(503).json({ error: 'Bot repository is not configured.' }); const [owner, repo] = String(item.repository).replace(/^https?:\/\/github\.com\//, '').replace(/\.git$/, '').split('/'); if (!owner || !repo) return res.status(400).json({ error: 'Invalid bot repository.' }); const data = await githubRequest(config, `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/actions/workflows?per_page=100`); res.json({ bot: item.slug, project: item.project, repository: `${owner}/${repo}`, workflows: data.workflows || [] }); } catch (error) { next(error); } });
  return router;
}
module.exports = { botsRoutes };
