const mongoose = require('mongoose');
const { getDatabaseConnection } = require('../db');

const customerSchema = new mongoose.Schema({
  name: { type: String, required: true, trim: true, maxlength: 160, index: true },
  externalId: { type: String, required: true, trim: true, maxlength: 160, index: true },
  email: { type: String, trim: true, lowercase: true, maxlength: 254, default: '' },
  phone: { type: String, trim: true, maxlength: 60, default: '' },
  status: { type: String, enum: ['active', 'inactive', 'prospect', 'blocked'], default: 'active', index: true },
  service: { type: String, trim: true, maxlength: 160, default: '' },
  registeredAt: { type: Date, default: Date.now },
  notes: { type: String, trim: true, maxlength: 5000, default: '' },
  history: [{ action: { type: String, trim: true, maxlength: 200 }, details: { type: String, trim: true, maxlength: 1000, default: '' }, at: { type: Date, default: Date.now } }]
}, { timestamps: true, collection: 'customers' });
customerSchema.index({ externalId: 1 }, { unique: true });

function getCustomerModel() {
  const connection = getDatabaseConnection('KorczakControl');
  if (!connection || connection.readyState !== 1) { const error = new Error('KorczakControl database connection is unavailable.'); error.statusCode = 503; throw error; }
  return connection.models.Customer || connection.model('Customer', customerSchema, 'customers');
}
module.exports = getCustomerModel;
