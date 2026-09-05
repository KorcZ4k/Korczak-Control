const mongoose = require('mongoose');
const crypto = require('crypto');

const databasePermissionsSchema = new mongoose.Schema({
  KorczakControl: { type: Boolean, default: false },
  MoonTensura: { type: Boolean, default: false },
  KorczakTechSite: { type: Boolean, default: false }
}, { _id: false });

const permissionsSchema = new mongoose.Schema({
  github: { type: Boolean, default: false },
  render: { type: Boolean, default: false },
  mongodb: { type: databasePermissionsSchema, default: () => ({}) },
  bots: { type: Boolean, default: false },
  sites: { type: Boolean, default: false },
  applications: { type: Boolean, default: false },
  apis: { type: Boolean, default: false }
}, { _id: false });

const resourcePermissionsSchema = new mongoose.Schema({
  department: { type: String, trim: true, default: '' },
  resource: { type: String, required: true, trim: true },
  permissions: { type: [String], default: [] }
}, { _id: false });

const userSchema = new mongoose.Schema({
  accountId: { type: String, unique: true, index: true, immutable: true },
  name: { type: String, required: true, trim: true, minlength: 2, maxlength: 120 },
  email: { type: String, required: true, unique: true, trim: true, lowercase: true, maxlength: 254 },
  passwordHash: { type: String, required: true, select: false },
  role: {
    type: String,
    enum: ['FOUNDER', 'ADMINISTRATOR', 'DEPARTMENT_MANAGER', 'DEVELOPER', 'STAFF', 'VIEWER'],
    default: 'VIEWER',
    index: true
  },
  department: { type: String, trim: true, default: '' },
  permissions: { type: permissionsSchema, default: () => ({}) },
  resourcePermissions: { type: [resourcePermissionsSchema], default: [] },
  active: { type: Boolean, default: true },
  lastLoginAt: { type: Date, default: null }
}, {
  timestamps: true,
  collection: process.env.ADMIN_COLLECTION_NAME || 'Users'
});

userSchema.pre('validate', function(next) {
  if (!this.accountId) this.accountId = `KZ-${crypto.randomBytes(6).toString('hex').toUpperCase()}`;
  next();
});

module.exports = mongoose.model('User', userSchema);
