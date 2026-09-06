const DEPARTMENTS = Object.freeze({
  KORCZAK_TECHNOLOGIES: 'Korczak Technologies',
  MOON_ROLEPLAYING: 'Moon Roleplaying'
});

const ROLES = Object.freeze({
  FOUNDER: { department: DEPARTMENTS.KORCZAK_TECHNOLOGIES, rank: 100, manages: true },
  DIRECTOR: { department: DEPARTMENTS.KORCZAK_TECHNOLOGIES, rank: 80, manages: true },
  MANAGER: { department: DEPARTMENTS.KORCZAK_TECHNOLOGIES, rank: 60, manages: true },
  OPERATIONAL: { department: DEPARTMENTS.KORCZAK_TECHNOLOGIES, rank: 20, manages: false },
  MOON_SUB_OWNER: { department: DEPARTMENTS.MOON_ROLEPLAYING, rank: 70, manages: true },
  MOON_HEAD_ADMIN: { department: DEPARTMENTS.MOON_ROLEPLAYING, rank: 60, manages: true },
  MOON_ADMIN: { department: DEPARTMENTS.MOON_ROLEPLAYING, rank: 40, manages: true },
  MOON_MODERATOR: { department: DEPARTMENTS.MOON_ROLEPLAYING, rank: 30, manages: false },
  MOON_HELPER: { department: DEPARTMENTS.MOON_ROLEPLAYING, rank: 20, manages: false },
  // Legacy roles retained for existing accounts.
  ADMINISTRATOR: { department: DEPARTMENTS.KORCZAK_TECHNOLOGIES, rank: 60, manages: true },
  DEPARTMENT_MANAGER: { department: DEPARTMENTS.KORCZAK_TECHNOLOGIES, rank: 60, manages: true },
  DEVELOPER: { department: DEPARTMENTS.KORCZAK_TECHNOLOGIES, rank: 20, manages: false },
  STAFF: { department: DEPARTMENTS.KORCZAK_TECHNOLOGIES, rank: 20, manages: false },
  VIEWER: { department: '', rank: 0, manages: false }
});

const ROLE_CODES = Object.freeze(Object.keys(ROLES));

function roleInfo(role) { return ROLES[role] || ROLES.VIEWER; }
function canManageRole(actorRole, targetRole) {
  const actor = roleInfo(actorRole);
  const target = roleInfo(targetRole);
  return actor.manages && actor.rank > target.rank;
}

const ORGANIZATION_TREE = Object.freeze({
  [DEPARTMENTS.KORCZAK_TECHNOLOGIES]: ['FOUNDER', 'DIRECTOR', 'MANAGER', 'OPERATIONAL'],
  [DEPARTMENTS.MOON_ROLEPLAYING]: ['MOON_SUB_OWNER', 'MOON_HEAD_ADMIN', 'MOON_ADMIN', 'MOON_MODERATOR', 'MOON_HELPER']
});

module.exports = { DEPARTMENTS, ROLES, ROLE_CODES, ORGANIZATION_TREE, roleInfo, canManageRole };
