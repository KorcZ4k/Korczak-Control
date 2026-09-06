const mongoose = require('mongoose');
const { getDatabaseConnection } = require('../db');

const siteSchema = new mongoose.Schema({
  name: { type: String, required: true, trim: true, maxlength: 120 },
  slug: { type: String, required: true, trim: true, lowercase: true, unique: true, match: /^[a-z0-9-]+$/ },
  url: { type: String, required: true, trim: true, maxlength: 2048 },
  repository: { type: String, trim: true, maxlength: 200, default: '' },
  technology: { type: String, trim: true, maxlength: 120, default: '' },
  status: { type: String, enum: ['operational', 'attention', 'unavailable', 'maintenance', 'unknown'], default: 'unknown', index: true },
  lastDeploymentAt: { type: Date, default: null },
  lastUpdatedAt: { type: Date, default: null },
  knownErrors: { type: [String], default: [] },
  notes: { type: String, trim: true, maxlength: 3000, default: '' }
}, { timestamps: true, collection: 'control_sites' });

function getSiteModel() {
  const connection = getDatabaseConnection('KorczakControl');
  if (!connection || connection.readyState !== 1) {
    const error = new Error('KorczakControl database connection is unavailable.');
    error.statusCode = 503;
    throw error;
  }
  return connection.models.ControlSite || connection.model('ControlSite', siteSchema, 'control_sites');
}

module.exports = getSiteModel;
