function loadConfig() {
  const environment = process.env.NODE_ENV || 'development';
  const port = Number(process.env.PORT || 3000);
  const jwtSecret = process.env.JWT_SECRET || process.env.JWT || '';

  const tensuraDbUri = process.env.TENSURA_DB_URI || '';
  const kzSiteDbUri = process.env.KZSITE_DB_URI || '';
  const adminDbUri = process.env.ADMIN_DB_URI || process.env.MONGODB_URI || '';

  const tensuraDbName = process.env.TENSURA_DB_NAME || 'TensuraMoon';
  const kzSiteDbName = process.env.KZSITE_DB_NAME || 'KorczakTechSite';
  const adminDbName = process.env.ADMIN_DB_NAME || 'KorczakControl';
  const adminCollectionName = process.env.ADMIN_COLLECTION_NAME || 'Users';

  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error('PORT must be a valid TCP port.');
  }
  if (jwtSecret.length < 32) {
    throw new Error('JWT_SECRET or JWT must contain at least 32 characters.');
  }
  if (!adminDbUri && environment === 'production') {
    throw new Error('ADMIN_DB_URI is required in production.');
  }

  return Object.freeze({
    environment,
    port,
    jwtSecret,
    adminDbUri,
    tensuraDbUri,
    kzSiteDbUri,
    adminDbName,
    adminCollectionName,
    tensuraDbName,
    kzSiteDbName,
    bootstrapToken: process.env.BOOTSTRAP_TOKEN || '',
    corsOrigin: process.env.CORS_ORIGIN || '',
    githubToken: process.env.GITHUB_TOKEN || '',
    renderApiKey: process.env.RENDER_API_KEY || '',
    kzSiteApi: process.env.KZSITE_API || '',
    kzControlApi: process.env.KZCONTROL_API || '',
    tensuraMoonGithubRepo: process.env.TENSURA_MOON_GITHUB_REPO || process.env.TENSURAMOON_GITHUB_REPO || '',
    serviceName: 'korczak-control-api',
    version: process.env.npm_package_version || '0.3.0'
  });
}

module.exports = { loadConfig };
