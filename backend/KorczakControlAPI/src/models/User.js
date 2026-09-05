const mongoose = require('mongoose');
const userSchema = new mongoose.Schema({
  name: { type: String, required: true, trim: true, minlength: 2, maxlength: 120 },
  email: { type: String, required: true, unique: true, trim: true, lowercase: true, maxlength: 254 },
  passwordHash: { type: String, required: true, select: false },
  role: { type: String, enum: ['Owner','Administrator','Developer','Manager','Viewer'], default: 'Viewer', index: true },
  active: { type: Boolean, default: true },
  lastLoginAt: { type: Date, default: null }
}, { timestamps: true, collection: 'users' });
module.exports = mongoose.model('User', userSchema);
