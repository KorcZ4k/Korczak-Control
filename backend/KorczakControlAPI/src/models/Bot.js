const mongoose = require('mongoose');
const { getDatabaseConnection } = require('../db');

const botSchema = new mongoose.Schema({
  name: { type: String, required: true, trim: true, maxlength: 160 },
  slug: { type: String, required: true, trim: true, lowercase: true, match: /^[a-z0-9-]+$/ },
  project: { type: String, required: true, trim: true, maxlength: 160, index: true },
  status: { type: String, enum: ['online', 'offline', 'restarting', 'updating', 'error', 'unknown'], default: 'unknown', index: true },
  version: { type: String, trim: true, maxlength: 100, default: '' },
  repository: { type: String, trim: true, maxlength: 200, default: '' },
  databaseKey: { type: String, trim: true, maxlength: 80, default: '' },
  lastUpdatedAt: { type: Date, default: null },
  lastError: { type: String, trim: true, maxlength: 4000, default: '' },
  notes: { type: String, trim: true, maxlength: 5000, default: '' }
}, { timestamps: true, collection: 'bots' });
botSchema.index({ slug: 1 }, { unique: true });

function getBotModel() {
  const connection = getDatabaseConnection('KorczakControl');
  if (!connection || connection.readyState !== 1) { const error = new Error('KorczakControl database connection is unavailable.'); error.statusCode = 503; throw error; }
  return connection.models.Bot || connection.model('Bot', botSchema, 'bots');
}
module.exports = getBotModel;
