const GITHUB_API = 'https://api.github.com';

async function githubRequest(config, path) {
  if (!config.githubToken) {
    const error = new Error('GitHub integration is not configured.');
    error.statusCode = 503;
    throw error;
  }
  const response = await fetch(`${GITHUB_API}${path}`, {
    headers: {
      Accept: 'application/vnd.github+json',
      Authorization: `Bearer ${config.githubToken}`,
      'X-GitHub-Api-Version': '2022-11-28',
      'User-Agent': 'Korczak-Control'
    }
  });
  if (!response.ok) {
    const error = new Error(`GitHub API request failed (${response.status}).`);
    error.statusCode = response.status >= 500 ? 502 : response.status;
    throw error;
  }
  return response.json();
}

module.exports = { githubRequest };
