const mongoose = require('mongoose');
const { getDatabaseConnection } = require('../db');

const managedResourceSchema = new mongoose.Schema({
  name: { type: String, required: true, trim: true, maxlength: 120 },
  slug: { type: String, required: true, trim: true, lowercase: true, match: /^[a-z0-9-]+$/ },
  kind: { type: String, required: true, enum: ['api', 'app'], index: true },
  url: { type: String, trim: true, maxlength: 2048, default: '' },
  repository: { type: String, trim: true, maxlength: 200, default: '' },
  technology: { type: String, trim: true, maxlength: 120, default: '' },
  version: { type: String, trim: true, maxlength: 80, default: '' },
  status: { type: String, enum: ['operational', 'attention', 'unavailable', 'maintenance', 'unknown'], default: 'unknown', index: true },
  latencyMs: { type: Number, min: 0, default: null },
  lastActivityAt: { type: Date, default: null },
  notes: { type: String, trim: true, maxlength: 3000, default: '' }
}, { timestamps: true, collection: 'control_resources' });
managedResourceSchema.index({ kind: 1, slug: 1 }, { unique: true });

function getManagedResourceModel() {
  const connection = getDatabaseConnection('KorczakControl');
  if (!connection || connection.readyState !== 1) {
    const error = new Error('KorczakControl database connection is unavailable.');
    error.statusCode = 503;
    throw error;
  }
  return connection.models.ManagedResource || connection.model('ManagedResource', managedResourceSchema, 'control_resources');
}

module.exports = getManagedResourceModel;
