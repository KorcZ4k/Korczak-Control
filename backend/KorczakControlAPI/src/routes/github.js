const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { githubRequest } = require('../integrations/github/client');

function githubRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  router.get('/status', (req, res) => res.json({ configured: Boolean(config.githubToken) }));
  router.get('/repos/:owner', async (req, res, next) => {
    try { res.json({ items: await githubRequest(config, `/users/${encodeURIComponent(req.params.owner)}/repos?per_page=100&sort=updated`) }); }
    catch (error) { next(error); }
  });
  router.get('/repos/:owner/:repo', async (req, res, next) => {
    try {
      const base = `/repos/${encodeURIComponent(req.params.owner)}/${encodeURIComponent(req.params.repo)}`;
      const [repository, commits, issues, pulls] = await Promise.all([
        githubRequest(config, base),
        githubRequest(config, `${base}/commits?per_page=10`),
        githubRequest(config, `${base}/issues?state=open&per_page=20`),
        githubRequest(config, `${base}/pulls?state=open&per_page=20`)
      ]);
      res.json({ repository, commits, issues: issues.filter((item) => !item.pull_request), pulls });
    } catch (error) { next(error); }
  });
  router.get('/repos/:owner/:repo/branches', async (req, res, next) => {
    try { res.json({ items: await githubRequest(config, `/repos/${encodeURIComponent(req.params.owner)}/${encodeURIComponent(req.params.repo)}/branches?per_page=100`) }); }
    catch (error) { next(error); }
  });
  return router;
}
module.exports = { githubRoutes };
