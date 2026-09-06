const express = require('express');
const { requireAuth, requireRole } = require('../middleware/auth');
const { githubRequest } = require('../integrations/github/client');

function githubRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));
  const repoBase = (owner, repo) => `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}`;
  const configuredRepo = () => {
    const value = String(config.tensuraMoonGithubRepo || '').trim().replace(/^https?:\/\/github\.com\//, '').replace(/\.git$/, '');
    const parts = value.split('/').filter(Boolean);
    return parts.length === 2 ? { owner: parts[0], repo: parts[1] } : null;
  };

  router.get('/status', (req, res) => res.json({ configured: Boolean(config.githubToken) }));
  router.get('/bots/tensura-moon/workflows', async (req, res, next) => {
    try {
      const target = configuredRepo();
      if (!target) return res.status(503).json({ error: 'Tensura Moon repository is not configured.', requestId: req.requestId });
      const workflows = await githubRequest(config, `${repoBase(target.owner, target.repo)}/actions/workflows?per_page=100`);
      res.json({ repository: `${target.owner}/${target.repo}`, workflows: workflows.workflows || [] });
    } catch (error) { next(error); }
  });
  router.get('/repos/:owner', async (req, res, next) => {
    try { res.json({ items: await githubRequest(config, `/users/${encodeURIComponent(req.params.owner)}/repos?per_page=100&sort=updated`) }); }
    catch (error) { next(error); }
  });
  router.get('/repos/:owner/:repo', async (req, res, next) => {
    try {
      const base = repoBase(req.params.owner, req.params.repo);
      const [repository, commits, issues, pulls] = await Promise.all([
        githubRequest(config, base), githubRequest(config, `${base}/commits?per_page=10`),
        githubRequest(config, `${base}/issues?state=open&per_page=20`), githubRequest(config, `${base}/pulls?state=open&per_page=20`)
      ]);
      res.json({ repository, commits, issues: issues.filter((item) => !item.pull_request), pulls });
    } catch (error) { next(error); }
  });
  router.get('/repos/:owner/:repo/branches', async (req, res, next) => { try { res.json({ items: await githubRequest(config, `${repoBase(req.params.owner, req.params.repo)}/branches?per_page=100`) }); } catch (error) { next(error); } });
  router.get('/repos/:owner/:repo/workflows', async (req, res, next) => { try { res.json(await githubRequest(config, `${repoBase(req.params.owner, req.params.repo)}/actions/workflows?per_page=100`)); } catch (error) { next(error); } });
  router.post('/repos/:owner/:repo/workflows/:workflowId/dispatch', requireRole('Owner', 'Administrator', 'Developer'), async (req, res, next) => {
    try { const ref = String(req.body?.ref || 'main'); await githubRequest(config, `${repoBase(req.params.owner, req.params.repo)}/actions/workflows/${encodeURIComponent(req.params.workflowId)}/dispatches`, { method: 'POST', body: { ref } }); res.status(202).json({ queued: true, workflowId: req.params.workflowId, ref }); } catch (error) { next(error); }
  });
  router.get('/repos/:owner/:repo/code', async (req, res, next) => {
    try { const path = String(req.query.path || ''); const suffix = path ? `/contents/${path.split('/').map(encodeURIComponent).join('/')}` : '/contents'; res.json(await githubRequest(config, `${repoBase(req.params.owner, req.params.repo)}${suffix}`)); } catch (error) { next(error); }
  });
  return router;
}
module.exports = { githubRoutes };
