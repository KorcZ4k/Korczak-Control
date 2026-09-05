const express = require('express');
const { requireAuth, requireRole } = require('../middleware/auth');
const { renderRequest } = require('../integrations/render/client');
function renderRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));
  router.get('/status', (req, res) => res.json({ configured: Boolean(config.renderApiKey) }));
  router.get('/services', async (req, res, next) => { try { res.json({ items: await renderRequest(config, '/services?limit=100') }); } catch (error) { next(error); } });
  router.get('/services/:serviceId', async (req, res, next) => { try { res.json(await renderRequest(config, `/services/${encodeURIComponent(req.params.serviceId)}`)); } catch (error) { next(error); } });
  router.get('/services/:serviceId/deploys', async (req, res, next) => { try { res.json({ items: await renderRequest(config, `/services/${encodeURIComponent(req.params.serviceId)}/deploys?limit=20`) }); } catch (error) { next(error); } });
  router.post('/services/:serviceId/deploys', requireRole('Owner', 'Administrator', 'Developer'), async (req, res, next) => { try { res.status(202).json(await renderRequest(config, `/services/${encodeURIComponent(req.params.serviceId)}/deploys`, { method: 'POST', body: { clearCache: Boolean(req.body?.clearCache) } })); } catch (error) { next(error); } });
  return router;
}
module.exports = { renderRoutes };
