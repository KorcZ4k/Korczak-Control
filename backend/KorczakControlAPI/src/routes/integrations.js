const express = require('express');
const { requireAuth } = require('../middleware/auth');
const { githubRequest } = require('../integrations/github/client');
const { renderRequest } = require('../integrations/render/client');

const CONNECTED = 'connected';
const CONFIGURED = 'configured';
const UNAVAILABLE = 'unavailable';
const NOT_CONFIGURED = 'not_configured';

function integration(id, name, status, detail) {
  return { id, name, status, detail };
}

async function verifyHttp(url) {
  if (!url) return null;
  try {
    const response = await fetch(url, { method: 'GET', signal: AbortSignal.timeout(8000) });
    return { ok: response.ok, status: response.status };
  } catch {
    return { ok: false };
  }
}

function integrationsRoutes(config) {
  const router = express.Router();
  router.use(requireAuth(config));

  router.get('/status', async (req, res) => {
    const items = [];

    // GitHub: a configured token is actively verified against the authenticated-user endpoint.
    if (!config.githubToken) {
      items.push(integration('github', 'GitHub', NOT_CONFIGURED, 'GITHUB_TOKEN ausente.'));
    } else {
      try {
        await githubRequest(config, '/user');
        items.push(integration('github', 'GitHub', CONNECTED, 'Token validado com a API do GitHub.'));
      } catch {
        items.push(integration('github', 'GitHub', UNAVAILABLE, 'Token configurado, mas a API não pôde ser autenticada.'));
      }
    }

    // Render: a configured key is verified by a real API request.
    if (!config.renderApiKey) {
      items.push(integration('render', 'Render', NOT_CONFIGURED, 'RENDER_API_KEY ausente.'));
    } else {
      try {
        await renderRequest(config, '/services?limit=1');
        items.push(integration('render', 'Render', CONNECTED, 'Chave validada com a API do Render.'));
      } catch {
        items.push(integration('render', 'Render', UNAVAILABLE, 'Chave configurada, mas a API não pôde ser autenticada.'));
      }
    }

    // MongoDB: this process already uses the configured administrative database connection.
    items.push(integration(
      'mongodb',
      'MongoDB',
      config.adminDbUri ? CONNECTED : NOT_CONFIGURED,
      config.adminDbUri ? 'ADMIN_DB_URI configurada para a API.' : 'ADMIN_DB_URI ausente.'
    ));

    // APIs are checked through their configured public endpoints. No URL is returned to clients.
    const kzControlCheck = await verifyHttp(config.kzControlApi);
    items.push(integration(
      'korczak_control_api',
      'Korczak-Control API',
      !config.kzControlApi ? NOT_CONFIGURED : kzControlCheck?.ok ? CONNECTED : UNAVAILABLE,
      !config.kzControlApi ? 'KZCONTROL_API ausente.' : kzControlCheck?.ok ? 'Endpoint respondeu com sucesso.' : 'Endpoint configurado, mas não respondeu com sucesso.'
    ));

    const kzSiteCheck = await verifyHttp(config.kzSiteApi);
    items.push(integration(
      'kz_site_api',
      'KZ Site API',
      !config.kzSiteApi ? NOT_CONFIGURED : kzSiteCheck?.ok ? CONNECTED : UNAVAILABLE,
      !config.kzSiteApi ? 'KZSITE_API ausente.' : kzSiteCheck?.ok ? 'Endpoint respondeu com sucesso.' : 'Endpoint configurado, mas não respondeu com sucesso.'
    ));

    // TensuraMoon is monitored through its repository identifier; workflow verification is added when the repo is configured.
    items.push(integration(
      'tensuramoon',
      'TensuraMoon',
      !config.tensuraMoonGithubRepo ? NOT_CONFIGURED : config.githubToken ? CONFIGURED : UNAVAILABLE,
      !config.tensuraMoonGithubRepo ? 'TENSURAMOON_GITHUB_REPO ausente.' : config.githubToken ? 'Repositório configurado; monitoramento do workflow pode ser ativado.' : 'Repositório configurado, mas o GitHub não está conectado.'
    ));

    items.push(integration('sites', 'Sites', config.kzSiteApi ? CONFIGURED : NOT_CONFIGURED, config.kzSiteApi ? 'KZ Site registrado pela API configurada.' : 'Nenhuma API de site configurada.'));
    items.push(integration('applications', 'Aplicações', config.kzControlApi ? CONFIGURED : NOT_CONFIGURED, config.kzControlApi ? 'Korczak-Control registrado pela API configurada.' : 'Nenhuma aplicação configurada.'));

    const counts = items.reduce((acc, item) => {
      acc[item.status] = (acc[item.status] || 0) + 1;
      return acc;
    }, {});

    return res.json({ items, counts, checkedAt: new Date().toISOString() });
  });

  return router;
}

module.exports = { integrationsRoutes };