const mongoose = require('mongoose');
const { getDatabaseConnection } = require('../db');

const controlEventSchema = new mongoose.Schema({
  category: { type: String, enum: ['deploy', 'service', 'database', 'security', 'system', 'resource'], default: 'system', index: true },
  severity: { type: String, enum: ['info', 'warning', 'error', 'critical'], default: 'info', index: true },
  title: { type: String, required: true, trim: true, maxlength: 180 },
  message: { type: String, trim: true, maxlength: 3000, default: '' },
  resourceType: { type: String, trim: true, maxlength: 80, default: '' },
  resourceId: { type: String, trim: true, maxlength: 200, default: '' },
  actorId: { type: String, trim: true, maxlength: 200, default: '' },
  readBy: { type: [String], default: [] },
  metadata: { type: mongoose.Schema.Types.Mixed, default: {} }
}, { timestamps: true, collection: 'control_events' });
controlEventSchema.index({ createdAt: -1 });

function getControlEventModel() {
  const connection = getDatabaseConnection('KorczakControl');
  if (!connection || connection.readyState !== 1) {
    const error = new Error('KorczakControl database connection is unavailable.');
    error.statusCode = 503;
    throw error;
  }
  return connection.models.ControlEvent || connection.model('ControlEvent', controlEventSchema, 'control_events');
}

module.exports = getControlEventModel;
