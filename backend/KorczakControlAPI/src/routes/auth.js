const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const User = require('../models/User');
const { requireAuth } = require('../middleware/auth');
function safeUser(user) { return { id: user._id.toString(), name: user.name, email: user.email, role: user.role, active: user.active }; }
function authRoutes(config) {
  const router = express.Router();
  router.post('/register', async (req,res,next)=>{ try {
    const existingCount=await User.countDocuments();
    if (config.environment === 'production' && existingCount > 0) return res.status(403).json({error:'Public registration is disabled.'});
    if (config.environment === 'production' && (!config.bootstrapToken || req.get('x-bootstrap-token') !== config.bootstrapToken)) return res.status(403).json({error:'Invalid bootstrap authorization.'});
    const {name,email,password}=req.body||{};
    if(typeof name!=='string'||typeof email!=='string'||typeof password!=='string'||name.trim().length<2||password.length<12) return res.status(400).json({error:'Invalid registration data. Password must contain at least 12 characters.'});
    const normalizedEmail=email.trim().toLowerCase();
    if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) return res.status(400).json({error:'Invalid email.'});
    if(await User.exists({email:normalizedEmail})) return res.status(409).json({error:'Email already registered.'});
    const user=await User.create({name:name.trim(),email:normalizedEmail,passwordHash:await bcrypt.hash(password,12),role:existingCount===0?'Owner':'Viewer'});
    return res.status(201).json({user:safeUser(user)});
  }catch(error){next(error);} });
  router.post('/login', async (req,res,next)=>{try{const{email,password}=req.body||{};if(typeof email!=='string'||typeof password!=='string')return res.status(400).json({error:'Invalid credentials.'});const user=await User.findOne({email:email.trim().toLowerCase()}).select('+passwordHash');if(!user||!user.active||!(await bcrypt.compare(password,user.passwordHash)))return res.status(401).json({error:'Invalid credentials.'});user.lastLoginAt=new Date();await user.save();const token=jwt.sign({sub:user._id.toString(),role:user.role},config.jwtSecret,{expiresIn:'8h',issuer:'korczak-control-api'});return res.json({token,user:safeUser(user)});}catch(error){next(error);}});
  router.get('/me',requireAuth(config),async(req,res,next)=>{try{const user=await User.findById(req.auth.sub);if(!user||!user.active)return res.status(401).json({error:'Session unavailable.'});return res.json({user:safeUser(user)});}catch(error){next(error);}});
  return router;
}
module.exports={authRoutes};
