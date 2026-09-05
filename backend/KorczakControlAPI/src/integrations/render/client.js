const RENDER_API = 'https://api.render.com/v1';
async function renderRequest(config, path, options = {}) {
  if (!config.renderApiKey) { const error = new Error('Render integration is not configured.'); error.statusCode = 503; throw error; }
  const response = await fetch(`${RENDER_API}${path}`, {
    method: options.method || 'GET',
    headers: { Authorization: `Bearer ${config.renderApiKey}`, Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}) },
    body: options.body ? JSON.stringify(options.body) : undefined
  });
  if (!response.ok) { const error = new Error(`Render API request failed (${response.status}).`); error.statusCode = response.status >= 500 ? 502 : response.status; throw error; }
  return response.json();
}
module.exports = { renderRequest };
