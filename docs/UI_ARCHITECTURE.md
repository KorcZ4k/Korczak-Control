# Arquitetura de Interface — Korczak Control

## Estrutura de telas

```text
Korczak Control
├── Dashboard
├── Sites
├── APIs
├── Aplicativos
├── Databases
├── Infraestrutura
├── Notificações
└── Configurações
```

## Fluxo principal

```text
Inicialização
   ↓
Autenticação
   ↓
Dashboard
   ├── Recursos
   ├── Alertas
   └── Atividade recente
```

A Etapa 2 define contratos visuais e de navegação. A implementação Kotlin com Jetpack Compose será criada quando o módulo Android for iniciado formalmente, evitando uma árvore Android manual incompleta ou incompatível com a versão do Gradle/SDK utilizada no ambiente de desenvolvimento.

## Estados obrigatórios

Cada tela conectada a dados deverá prever:
- carregamento;
- conteúdo;
- vazio;
- erro;
- indisponibilidade parcial.

## Segurança visual

A interface não exibirá tokens, senhas, chaves de API ou credenciais de infraestrutura. Informações sensíveis serão representadas por status e metadados seguros.
