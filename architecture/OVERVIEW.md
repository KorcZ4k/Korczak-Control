# Arquitetura — Korczak Control

## Visão geral

```text
Aplicativo Korczak Control
          |
          | HTTPS
          v
Korczak Control API
          |
          +-- GitHub
          +-- Render
          +-- MongoDB
          +-- futuros serviços
```

## Regra de segurança

O aplicativo cliente nunca deve receber tokens administrativos, chaves privadas ou credenciais de infraestrutura.

Todas as integrações externas serão realizadas futuramente pela Korczak Control API.

## Módulos planejados

- auth
- dashboard
- sites
- apis
- apps
- databases
- infrastructure
- notifications
- users
- settings
- integrations
