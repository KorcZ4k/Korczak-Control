# Integração total do Korczak Control

## Fluxo real

```text
Android
  ↓
URL configurada pelo usuário administrativo
  ↓
POST /api/auth/login
  ↓
JWT armazenado localmente
  ↓
GET /api/auth/me na inicialização
  ↓
Navegação autenticada
  ├── Dashboard
  ├── Sites
  ├── APIs
  ├── Aplicativos
  ├── MongoDB
  ├── GitHub
  ├── Render
  └── Eventos
```

## Configuração inicial

1. Inicie a Korczak Control API.
2. Configure `MONGODB_URI`, `JWT_SECRET` e, quando aplicável, `GITHUB_TOKEN` e `RENDER_API_KEY`.
3. Abra o aplicativo.
4. Informe a URL HTTPS da API.
5. Entre com uma conta existente.

## Segurança

O aplicativo não recebe tokens do GitHub, Render ou MongoDB. Esses segredos permanecem exclusivamente no backend. O token de sessão é enviado apenas no cabeçalho `Authorization: Bearer` para a Korczak Control API.

## Estado das integrações

- GitHub e Render podem permanecer como `not-configured` até que suas chaves sejam adicionadas ao ambiente do backend.
- MongoDB aparece como conectado somente quando o processo da API estabelece conexão real.
- O dashboard calcula recursos e status a partir dos dados persistidos em vez de retornar contagens estáticas.
