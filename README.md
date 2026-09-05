# Korczak Control

Painel central de administração da infraestrutura da Korczak Technologies.

## Etapa 1 — Fundação

Nesta fase o projeto possui uma estrutura inicial independente para:

- aplicativo Android;
- API central;
- documentação;
- arquitetura;
- configuração segura de ambiente;
- health check.

## Estrutura

```text
Korczak-Control/
├── android/
│   └── KorczakControl/
├── architecture/
├── backend/
│   └── KorczakControlAPI/
├── docs/
└── README.md
```

## Backend

```bash
cd backend/KorczakControlAPI
npm install
cp .env.example .env
npm run dev
```

Health check:

```text
GET /health
```

## Segurança

Credenciais, tokens e segredos não devem ser enviados ao GitHub. Use o arquivo `.env` apenas localmente e configure variáveis de ambiente diretamente no serviço de hospedagem.
