const mongoose = require('mongoose');
const { getDatabaseConnection } = require('../db');

const applicationSchema = new mongoose.Schema({
  name: { type: String, required: true, trim: true, maxlength: 160 },
  slug: { type: String, required: true, trim: true, lowercase: true, match: /^[a-z0-9-]+$/ },
  status: { type: String, enum: ['operational', 'attention', 'unavailable', 'maintenance', 'unknown'], default: 'unknown', index: true },
  version: { type: String, trim: true, maxlength: 100, default: '' },
  platforms: [{ type: String, trim: true, maxlength: 40 }],
  repository: { type: String, trim: true, maxlength: 200, default: '' },
  apiUrl: { type: String, trim: true, maxlength: 2048, default: '' },
  siteUrl: { type: String, trim: true, maxlength: 2048, default: '' },
  databaseKey: { type: String, trim: true, maxlength: 80, default: '' },
  botSlug: { type: String, trim: true, maxlength: 120, default: '' },
  lastUpdatedAt: { type: Date, default: null },
  notes: { type: String, trim: true, maxlength: 5000, default: '' },
  history: [{ event: { type: String, trim: true, maxlength: 200 }, at: { type: Date, default: Date.now }, details: { type: String, trim: true, maxlength: 1000, default: '' } }]
}, { timestamps: true, collection: 'applications' });
applicationSchema.index({ slug: 1 }, { unique: true });

function getApplicationModel() {
  const connection = getDatabaseConnection('KorczakControl');
  if (!connection || connection.readyState !== 1) { const error = new Error('KorczakControl database connection is unavailable.'); error.statusCode = 503; throw error; }
  return connection.models.Application || connection.model('Application', applicationSchema, 'applications');
}
module.exports = getApplicationModel;
