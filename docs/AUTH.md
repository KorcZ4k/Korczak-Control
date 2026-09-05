# Autenticação — Korczak Control

## Rotas
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`

## Papéis
- Owner
- Administrator
- Developer
- Manager
- Viewer

## Primeiro Owner
Em produção, o registro público não fica aberto. A criação do primeiro usuário exige `BOOTSTRAP_TOKEN` e o header `X-Bootstrap-Token`. Depois que existir um usuário, novos usuários deverão ser administrados por rotas internas futuras.

## Sessão
O login emite um JWT com validade de 8 horas. Clientes devem enviá-lo em `Authorization: Bearer <token>`.

## Segurança
- Senhas são armazenadas apenas como hash bcrypt.
- Tokens e credenciais não são retornados em perfis.
- O aplicativo Android não deve conter segredos administrativos.
