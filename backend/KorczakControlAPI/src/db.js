const mongoose = require('mongoose');

const connections = new Map();

async function connectDatabase(key, uri, dbName) {
  if (!uri) return null;

  const existing = connections.get(key);
  if (existing && existing.readyState === 1) return existing;

  const connection = mongoose.createConnection(uri, {
    dbName,
    serverSelectionTimeoutMS: 10000
  });

  await connection.asPromise();
  connections.set(key, connection);
  console.log(`MongoDB connected: ${key} -> ${connection.name}.`);
  return connection;
}

function getDatabaseConnection(key) {
  return connections.get(key) || null;
}

function getDatabaseConnections() {
  return new Map(connections);
}

function getDatabase(key) {
  const connection = getDatabaseConnection(key);
  if (!connection || connection.readyState !== 1) return null;
  return connection.db;
}

module.exports = {
  connectDatabase,
  getDatabaseConnection,
  getDatabaseConnections,
  getDatabase
};
