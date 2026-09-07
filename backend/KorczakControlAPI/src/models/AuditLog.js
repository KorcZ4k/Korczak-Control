const mongoose = require('mongoose');
const { getDatabaseConnection } = require('../db');

const auditLogSchema = new mongoose.Schema({
  actorAccountId: { type: String, default: '' },
  actorRole: { type: String, default: '' },
  targetAccountId: { type: String, default: '' },
  action: { type: String, required: true, trim: true },
  details: { type: mongoose.Schema.Types.Mixed, default: {} }
}, { timestamps: true, collection: 'AuditLogs' });

auditLogSchema.index({ targetAccountId: 1, createdAt: -1 });
auditLogSchema.index({ actorAccountId: 1, createdAt: -1 });

function getAuditLogModel() {
  const connection = getDatabaseConnection('KorczakControl');
  if (!connection || connection.readyState !== 1) {
    const error = new Error('KorczakControl database connection is unavailable.');
    error.statusCode = 503;
    throw error;
  }
  return connection.models.AuditLog || connection.model('AuditLog', auditLogSchema, 'AuditLogs');
}

module.exports = getAuditLogModel;
