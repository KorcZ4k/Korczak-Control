const jwt = require('jsonwebtoken');
function requireAuth(config) {
  return (req, res, next) => {
    const header = req.get('authorization') || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : '';
    if (!token) return res.status(401).json({ error: 'Authentication required.', requestId: req.requestId });
    try { req.auth = jwt.verify(token, config.jwtSecret); return next(); }
    catch { return res.status(401).json({ error: 'Invalid or expired session.', requestId: req.requestId }); }
  };
}
function requireRole(...roles) {
  return (req, res, next) => roles.includes(req.auth?.role) ? next() : res.status(403).json({ error: 'Insufficient permission.', requestId: req.requestId });
}
module.exports = { requireAuth, requireRole };
