# Produção — Korczak Control

## Backend

Antes do deploy, configure no ambiente:

- `NODE_ENV=production`
- `PORT`
- `MONGODB_URI`
- `JWT_SECRET` com no mínimo 32 caracteres
- `CORS_ORIGIN` com as origens autorizadas
- `GITHUB_TOKEN`, quando a integração GitHub for utilizada
- `RENDER_API_KEY`, quando a integração Render for utilizada

Nenhuma credencial deve ser enviada ao aplicativo Android ou commitada no repositório.

## Verificações antes do lançamento

1. Execute a CI com sucesso.
2. Confirme `GET /health`.
3. Confirme conexão do MongoDB.
4. Teste login e rota autenticada.
5. Teste permissões por papel.
6. Verifique GitHub e Render com credenciais de ambiente.
7. Teste rotas de sites, APIs, aplicativos, databases e eventos.
8. Revise CORS para remover origens de desenvolvimento desnecessárias.
9. Gere e teste uma versão Android em dispositivo real.
10. Faça backup da configuração operacional necessária.

## Lançamento Android

O APK/AAB de produção deve ser gerado somente depois de uma build local bem-sucedida e de testes em dispositivo real. A chave de assinatura Android deve permanecer fora do GitHub e nunca ser incluída no repositório.

## Limite atual da etapa

A etapa de produção prepara o projeto para CI e deploy, mas não cria uma conta de loja, domínio ou assinatura de publicação automaticamente. Essas ações dependem das contas e credenciais do proprietário.
