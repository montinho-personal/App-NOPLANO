# NO PLANO — Arquitetura

```
Interface (Compose)  ──►  ViewModel  ──►  Repositorio  ──►  Room (SQLite)
        ▲                                     │
        │                                     ├──►  DataStore (ajustes)
   Widget (Glance) ◄──── aoMudar ◄────────────┤
   Lembretes (AlarmManager) ◄─────────────────┘
```

- **Uma porta de entrada para os dados:** o `Repositorio`. Depois de cada gravação ele
  avisa (`aoMudar`), e o app atualiza os widgets e reagenda o próximo lembrete.
- **Lógica separada do Android:** tudo em `domain/` é Kotlin puro e testado em segundos.
- **Telas reativas:** a tela observa o banco (`Flow`); registrar muda os números sozinho.

## Dados

| Tabela | O que guarda |
|---|---|
| `Alimento` | Valores por 100 g ou 100 ml, com a fonte (TACO, Rótulo, Manual) |
| `Plano`, `DiaSemana` | Planos por tipo de dia, alvo de macros opcional, e qual plano vale em cada dia da semana |
| `Refeicao`, `Opcao`, `ItemPlano` | Refeições, suas alternativas e os alimentos de cada uma |
| `Registro`, `ItemRegistro` | O que foi consumido, com os valores do momento do registro |
| `Agua` | Cada adição de água |
| `DiaResumo` | A meta planejada em cada dia com registro, e o motivo opcional do dia difícil |
| `Conquista` | Conquistas alcançadas e quando |

Planejado e consumido ficam em tabelas diferentes. Por isso, mudar o plano não reescreve
o passado, e o relatório compara cada dia com a meta que valia naquele dia.

## Ajuste pelas macros

Na tela Plano, o painel "Macros do plano" soma todas as refeições e compara com o alvo
que o usuário digitou. No editor de refeição, o topo fica fixo e mostra o dia inteiro
contra o alvo; cada toque em – ou + grava a nova quantidade e os números mudam na hora.
O passo dos botões depende da quantidade (1 g abaixo de 20 g, 5 g até 60 g, 10 g acima),
ou é uma unidade quando o item está em medida caseira. Ao escolher um alimento novo ou
digitar uma quantidade, a mesma comparação aparece antes de salvar.

## Banco: versões

| Versão | Mudança |
|---|---|
| 1 | Primeira versão |
| 2 | Alvo de macros opcional em `Plano` (`MIGRACAO_1_2`) |

## Lembretes

Existe sempre **um** alarme agendado: o do próximo lembrete do dia, ou a virada do dia.
Quando ele dispara, a regra é recalculada com os dados daquele momento. Se a refeição já
foi registrada, nada aparece. Em seguida o próximo é agendado. Uma tarefa do WorkManager
confere tudo a cada 6 horas, como rede de segurança, e os alarmes são recriados depois de
reiniciar o celular.

## Widgets

Os widgets leem o mesmo estado do dia que o app. Toda gravação pede a atualização na
hora. Os botões **Concluí** e **+250 ml** gravam direto, sem abrir o app. O número grande
e a barra são desenhados como imagem, porque o Glance só oferece fontes do sistema.

## Testes

| Arquivo | Cobre |
|---|---|
| `CalculoTest` | Calorias, macros, porções parciais, arredondamento, estado do dia |
| `LembretesTest` | Regras de notificação e conteúdo do widget |
| `ConsistenciaTest` | Sequência, retomadas, conquistas, relatórios, padrões |
| `AlvoTest` | Comparação com o alvo e passos dos botões – e + |
| `MigracaoTest` | Banco da versão 1 aberto pela versão atual, sem perder dados |
| `RepositorioTest` | Banco de verdade (em memória): duplicidade, desfazer, virada do dia, plano por dia da semana, backup e restauração, tabela TACO |
| `TelasTest` | Desenha as telas reais e salva em `docs/telas` |
