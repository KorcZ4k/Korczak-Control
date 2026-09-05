const requiredInProduction = ['JWT_SECRET'];

function loadConfig() {
  const environment = process.env.NODE_ENV || 'development';
  const port = Number(process.env.PORT || 3000);

  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error('PORT must be a valid TCP port.');
  }

  if (environment === 'production') {
    for (const key of requiredInProduction) {
      const value = process.env[key];
      if (!value || value.length < 32) {
        throw new Error(`${key} must be configured with at least 32 characters in production.`);
      }
    }
  }

  return Object.freeze({
    environment,
    port,
    corsOrigin: process.env.CORS_ORIGIN || '',
    serviceName: 'korczak-control-api',
    version: process.env.npm_package_version || '0.1.0'
  });
}

module.exports = { loadConfig };
