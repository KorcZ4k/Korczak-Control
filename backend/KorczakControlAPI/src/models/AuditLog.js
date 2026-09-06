const mongoose = require('mongoose');

const auditLogSchema = new mongoose.Schema({
  actorAccountId: { type: String, default: '' },
  actorRole: { type: String, default: '' },
  targetAccountId: { type: String, default: '' },
  action: { type: String, required: true, trim: true },
  details: { type: mongoose.Schema.Types.Mixed, default: {} }
}, { timestamps: true, collection: 'AuditLogs' });

auditLogSchema.index({ targetAccountId: 1, createdAt: -1 });
module.exports = mongoose.model('AuditLog', auditLogSchema);
