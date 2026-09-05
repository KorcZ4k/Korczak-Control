# Sistema de Interface — Korczak Control

## Direção visual
Interface administrativa escura, precisa e minimalista. A hierarquia deve priorizar estado operacional, legibilidade e ações críticas.

## Princípios
- Preto e tons neutros como base.
- Uma cor de destaque controlada para ações e estados importantes.
- Espaçamento generoso e componentes consistentes.
- Status nunca depende apenas de cor: sempre possui texto ou ícone.
- Estados de carregamento, vazio e erro fazem parte da interface.

## Navegação
- Dashboard
- Sites
- APIs
- Aplicativos
- Databases
- Infraestrutura
- Notificações
- Configurações

## Componentes
### AppTopBar
Identidade da tela e ações contextuais.

### StatusCard
Nome do recurso, estado, detalhes e última atualização.

### MetricCard
Métrica principal, valor e contexto.

### ResourceRow
Item navegável para serviços, APIs, bancos ou aplicativos.

### EmptyState
Mensagem clara, motivo e ação possível.

### ErrorState
Falha, contexto e ação de tentar novamente.

### LoadingState
Indicador de carregamento sem bloquear a compreensão da tela.

## Estados semânticos
- Operacional
- Atenção
- Indisponível
- Desconhecido
- Em manutenção

## Acessibilidade
- Alvos de toque adequados.
- Contraste suficiente.
- Conteúdo compreensível por leitores de tela.
- Não utilizar apenas cor para comunicar status.
