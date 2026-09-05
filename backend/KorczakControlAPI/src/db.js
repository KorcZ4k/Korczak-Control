const mongoose = require('mongoose');
async function connectDatabase(uri) {
  if (!uri) return false;
  await mongoose.connect(uri, { serverSelectionTimeoutMS: 10000 });
  console.log('MongoDB connected.');
  return true;
}
module.exports = { connectDatabase };
