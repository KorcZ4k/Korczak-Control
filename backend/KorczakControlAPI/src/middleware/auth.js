const jwt = require('jsonwebtoken');
const { roleInfo } = require('../config/organization');

function requireAuth(config) {
  return (req, res, next) => {
    const header = req.get('authorization') || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : '';
    if (!token) return res.status(401).json({ error: 'Authentication required.', requestId: req.requestId });
    try { req.auth = jwt.verify(token, config.jwtSecret); return next(); }
    catch { return res.status(401).json({ error: 'Invalid or expired session.', requestId: req.requestId }); }
  };
}

const ROLE_THRESHOLDS = Object.freeze({ Owner: 100, Administrator: 60, Developer: 20 });
function requireRole(...roles) {
  const minimumRank = Math.min(...roles.map((role) => ROLE_THRESHOLDS[role] ?? 101));
  return (req, res, next) => {
    const role = req.auth?.role;
    if (roles.includes(role)) return next();
    const info = roleInfo(role);
    return info.rank >= minimumRank ? next() : res.status(403).json({ error: 'Insufficient permission.', requestId: req.requestId });
  };
}
module.exports = { requireAuth, requireRole };
