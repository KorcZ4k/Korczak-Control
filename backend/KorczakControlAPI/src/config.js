function loadConfig() {
  const environment = process.env.NODE_ENV || 'development';
  const port = Number(process.env.PORT || 3000);
  const jwtSecret = process.env.JWT_SECRET || '';
  const mongoUri = process.env.MONGODB_URI || '';
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('PORT must be a valid TCP port.');
  if (jwtSecret.length < 32) throw new Error('JWT_SECRET must contain at least 32 characters.');
  if (!mongoUri && environment === 'production') throw new Error('MONGODB_URI is required in production.');
  return Object.freeze({ environment, port, jwtSecret, mongoUri, bootstrapToken: process.env.BOOTSTRAP_TOKEN || '', corsOrigin: process.env.CORS_ORIGIN || '', serviceName: 'korczak-control-api', version: process.env.npm_package_version || '0.2.0' });
}
module.exports = { loadConfig };
