const mongoose = require('mongoose');

async function connectDatabase(uri, dbName) {
  if (!uri) return false;
  await mongoose.connect(uri, {
    dbName,
    serverSelectionTimeoutMS: 10000
  });
  console.log(`MongoDB connected to ${dbName}.`);
  return true;
}

module.exports = { connectDatabase };
