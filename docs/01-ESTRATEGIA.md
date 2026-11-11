# NO PLANO — Estratégia (Etapa 1)

> Documento produzido a partir do conhecimento disponível, assumindo seis perspectivas
> profissionais (design de produto, neurociência da motivação, psicologia comportamental,
> nutrição esportiva, gamificação e engenharia Android). **Nenhum especialista real foi
> consultado.** Os pontos que pedem validação profissional estão marcados no fim.

Ao longo do texto, cada afirmação relevante leva uma etiqueta:

- **[Evidência]** — resultado sustentado por literatura científica revisada por pares.
- **[Hipótese]** — aposta de design plausível, ainda não demonstrada para este produto.
- **[Decisão]** — escolha de produto, feita por critério de simplicidade, custo ou gosto.

---

## 1. Análise crítica do conceito

**O que o conceito tem de forte**

- A dieta já está decidida. O app não precisa descobrir o que a pessoa comeu; precisa
  só confirmar que o plano foi executado. Isso troca "digitar alimentos" por "um toque",
  que é o atrito que mais derruba apps de registro alimentar. **[Hipótese]**, apoiada em
  **[Evidência]**: a frequência de automonitoramento se associa de forma consistente a
  melhores resultados, e ela cai com o tempo quando registrar dá trabalho
  (Burke et al., 2011, revisão sistemática).
- Um usuário, um aparelho, sem servidor: custo zero é realista e a privacidade é total.
  **[Decisão]**
- Widget e notificações colocam o plano onde a atenção já está (a tela inicial do
  celular), em vez de depender de a pessoa lembrar de abrir o app. **[Hipótese]**

**Riscos e como o projeto responde**

| Risco | Resposta |
|---|---|
| Virar "contador de calorias" de novo, com números demais | A tela principal responde seis perguntas e nada mais; o detalhe fica um nível abaixo. **[Decisão]** |
| Gamificação que premia comer menos ou pune um dia ruim | Só se recompensa registrar, seguir o plano e retomar. Ficar abaixo da meta nunca rende nada. A consistência não zera. **[Decisão]** |
| Perfeccionismo: um dia fora vira abandono | Mecanismo de retomada como função de primeira classe (seção 3, obstáculo 8). **[Evidência]** do fenômeno; **[Hipótese]** da solução |
| Excesso de notificações gerando rejeição | Teto diário, período de silêncio e regra "não lembrar o que já foi registrado". **[Decisão]** |
| Promessa de "ciência" inflada | Este documento separa evidência de hipótese; o app não usa linguagem de "reprogramar o cérebro". **[Decisão]** |
| Sinais de relação difícil com a comida | O app não diagnostica nem trata. Não há metas de restrição, nem elogio a déficit. Se o uso gerar ansiedade, a orientação é procurar um profissional. **[Decisão]** — ponto que pede validação |
| Widget e alarmes limitados pelo Android | Estratégia técnica na seção 7, com o que o sistema permite de fato. **[Evidência técnica]** |

**Limite honesto do produto:** o app aumenta a visibilidade do plano e reduz o esforço
de segui-lo. Ele não garante adesão, não substitui acompanhamento nutricional e não
calcula a dieta de ninguém.

---

## 2 e 3. Dez obstáculos à adesão e a estratégia para cada um

| # | Obstáculo | O que se sabe | Estratégia no app |
|---|---|---|---|
| 1 | **Esquecimento** | **[Evidência]** Planos "se X, então Y" ligados a um gatilho concreto aumentam a execução de intenções (Gollwitzer & Sheeran, 2006, meta-análise). | Cada refeição tem horário. O lembrete chega antes dela e o widget mostra sempre "a próxima". **[Hipótese]** de que o horário funciona como gatilho. |
| 2 | **Dificuldade de registrar** | **[Evidência]** A adesão ao automonitoramento cai quando o registro é trabalhoso (Burke et al., 2011). | Registro em um toque, sem confirmação, com desfazer. Também pelo widget e pela notificação. **[Decisão]** |
| 3 | **Fadiga decisória** | **[Evidência]** Opções-padrão mudam o comportamento (literatura de arquitetura de escolha). A teoria do "esgotamento do ego" tem replicação fraca, então não nos apoiamos nela. | O plano é o padrão: o app já diz o que comer e quanto. Alternativas são pré-cadastradas, não escolhidas na hora. **[Hipótese]** |
| 4 | **Fome não planejada** | **[Hipótese]** Saber quanto falta para a próxima refeição ajuda a esperar ou a antecipar com consciência. | A próxima refeição fica sempre visível com o horário. É possível antecipá-la ou registrar um lanche fora do plano sem julgamento. |
| 5 | **Impulsividade** | **[Evidência]** Registrar o que se comeu faz parte das intervenções comportamentais eficazes; o efeito isolado de cada componente é incerto. | "Registrar fora do plano" é rápido e neutro. O total do dia se atualiza e a mensagem aponta para a próxima refeição, sem propor compensação. **[Decisão]** |
| 6 | **Cansaço** | **[Hipótese]** No fim do dia sobra pouca energia para lidar com interfaces. | A última refeição se registra com o mesmo toque único. O resumo da noite é uma frase e um gráfico, não um questionário. |
| 7 | **Rotina imprevisível** | **[Hipótese]** Planos rígidos quebram em dias atípicos. | Planos por tipo de dia (ex.: treino, descanso) atribuídos aos dias da semana; horários editáveis; registro parcial; alternativas por refeição. **[Decisão]** |
| 8 | **Pensamento de tudo ou nada** | **[Evidência]** O "efeito de violação da abstinência" (Marlatt & Gordon) e o padrão "já que saí, tanto faz" (Polivy & Herman) descrevem como um deslize interpretado como fracasso antecede o abandono. | Mecanismo de retomada: "Seu próximo passo é a próxima refeição". Nenhum contador zera. Existe conquista para a retomada. **[Hipótese]** da eficácia. |
| 9 | **Falta de feedback** | **[Evidência]** Feedback melhora o desempenho em média, mas com grande variação; feedback que ameaça a autoimagem pode piorar (Kluger & DeNisi, 1996). | Feedback imediato, factual e sobre a tarefa: anel preenchendo, números atualizando, frase curta sobre o progresso real. Nunca sobre a pessoa. **[Decisão]** |
| 10 | **Perda de motivação e frustração ao longo dos meses** | **[Evidência]** Motivação duradoura depende de autonomia e sensação de competência (teoria da autodeterminação, Deci & Ryan). Hábitos ganham automaticidade com repetição em contexto estável; faltar uma vez não compromete o processo (Lally et al., 2010). | Mostrar competência acumulada: semana em movimento, tendência mensal, conquistas de continuidade. O usuário controla tudo (metas, horários, lembretes). **[Hipótese]** |

**Sobre "dopamina" e recompensa.** Sabe-se, de pesquisa básica, que sinais de recompensa
no cérebro respondem à diferença entre o esperado e o obtido (Schultz, 1997) **[Evidência]**.
Daí não se conclui que uma animação "libera dopamina e cria hábito"; essa extrapolação
não é sustentada. Tratamos as microanimações como **[Hipótese]** de design: um retorno
imediato e agradável torna o registro menos aversivo. O objetivo é adesão ao plano, não
tempo de tela; por isso não há recompensa variável, feed, nem motivo para abrir o app
fora das refeições.

---

## 4. Três direções de identidade visual

Detalhadas e comparadas em [02-DESIGN.md](02-DESIGN.md), com imagens em `docs/design/`.

1. **Grafite** — sofisticação minimalista.
2. **Pulso** — energia esportiva premium.
3. **Trilha** — bem-estar e consistência.

Direção escolhida: **Grafite**, incorporando a ideia de "trilha do dia" da terceira.

---

## 5. Experiência da tela principal — "Meu Dia"

Uma tela, sem rolagem obrigatória para o essencial, que responde nesta ordem:

1. **Quanto já comi e quanto falta?** O *Anel do Plano*: um anel dividido em segmentos,
   um por refeição planejada, com largura proporcional às calorias de cada uma. Concluir
   uma refeição preenche o segmento dela. No centro, `1.450 / 2.000 kcal` e "faltam 550".
   Um único desenho mostra calorias, quantas refeições foram feitas e qual é a próxima.
2. **Macros:** três barras finas (proteínas, carboidratos, gorduras) com `consumido / meta`.
3. **Próxima refeição:** cartão com horário, itens e o botão grande **CONCLUÍ REFEIÇÃO**.
4. **Água:** `2,1 / 3,0 L` e botões `+150`, `+250`, `+500` e "outro".
5. **Refeições do dia:** `3 de 5 concluídas`, lista compacta com estado de cada uma.
6. **Mensagem contextual:** uma frase baseada no estado real do dia.
7. **Semana:** sete pontos no topo mostram a consistência; tocar leva ao Progresso.

**[Decisão]** A meta do dia é a soma do plano cadastrado para aquele dia. O app não
calcula metas por conta própria.

**[Decisão]** Passar da meta não fica vermelho nem dispara alerta. O número apenas
mostra o valor real; a cor de alerta fica reservada a erros do sistema.

---

## 6. Fluxo ideal de registro de uma refeição

```
Toque em CONCLUÍ REFEIÇÃO
  ├─ grava o consumo (itens e quantidades do plano)             < 100 ms
  ├─ anel, macros e contagem animam até os novos valores        ~ 600 ms
  ├─ vibração curta (se habilitada)
  ├─ widget atualizado na mesma operação
  ├─ lembretes dessa refeição cancelados
  └─ faixa inferior: "Almoço registrado · faltam 2 refeições"  [DESFAZER]  (6 s)
```

- Sem diálogo de confirmação. O erro se corrige com **Desfazer**.
- Toque longo no botão (ou em "ajustar") abre a folha de **consumo parcial**: 25%, 50%,
  75% ou quantidade por item. Trocar um item por uma alternativa acontece ali também.
- Um segundo toque acidental não duplica: cada refeição do plano tem no máximo um
  registro por dia; tocar de novo abre a edição.
- O mesmo registro funciona pelo widget e pela notificação, sem abrir o app.

---

## 7. Widgets e notificações no Android

**Widgets (Jetpack Glance)**

- Dois tamanhos: **compacto** (calorias + próxima refeição) e **expandido** (calorias,
  proteínas, água, próxima refeição, progresso do dia, botões "Concluí" e "+250 ml").
- Atualização imediata: toda gravação no banco chama a atualização do widget na mesma
  operação. Além disso, uma tarefa agendada atualiza na virada do dia e a cada refeição.
- O que aparece é configurável nos Ajustes.
- **Limites reais:** o Android não permite widgets de terceiros na tela de bloqueio dos
  celulares atuais (essa função foi removida no Android 5 e voltou só para tablets no
  Android 15 QPR1). Alguns fabricantes atrasam a atualização de widgets quando o app
  está em economia de bateria. O app não promete exibição permanente na tela de bloqueio.
  Na tela de bloqueio, o que aparece são as notificações.

**Notificações (regras locais, sem IA)**

| Categoria | Regra | Padrão |
|---|---|---|
| Antes da refeição | X min antes do horário, se ainda não registrada | 10 min |
| Refeição pendente | Y min depois do horário, se ainda não registrada; uma vez só | 45 min |
| Hidratação | Em intervalos, só se o consumo estiver abaixo do ritmo esperado para a hora | a cada 2 h |
| Resumo do dia | Uma vez, à noite | 21h30 |

- Antes de exibir, a regra consulta o banco: refeição registrada não gera lembrete.
- Período de silêncio configurável, teto diário de notificações e botão "Concluí" na
  própria notificação.
- **Precisão:** `AlarmManager` com alarme exato quando o usuário concede a permissão
  "Alarmes e lembretes" (Android 12+); sem ela, alarme aproximado com janela curta.
  `WorkManager` cuida do que pode esperar (reagendar o dia seguinte, atualizar widget).
- **Permissões:** `POST_NOTIFICATIONS` (Android 13+) é pedida com explicação.
  Os alarmes são recriados depois de reiniciar o celular. O app orienta a liberar a
  restrição de bateria quando o fabricante é agressivo, sem exigir.

---

## 8. Escopo

**MVP (esta versão)**

1. Cadastro da dieta: planos por tipo de dia, refeições, horários, alimentos,
   alternativas, duplicar refeição e plano.
2. Base de alimentos: tabela TACO (NEPA/UNICAMP) embutida + alimentos próprios (rótulo).
3. Registro em um toque, parcial, fora do plano, desfazer e correção de dias anteriores.
4. Cálculo de calorias e macros com arredondamento consistente.
5. Água.
6. Tela "Meu Dia".
7. Widgets compacto e expandido.
8. Notificações por regras.
9. Persistência local (Room) e backup/restauração em arquivo.
10. Consistência, semana em movimento, conquistas, retomada, relatórios semanal e mensal,
    observações de padrões.

**Fica para depois**

- Leitura de código de barras e foto do rótulo.
- Micronutrientes e fibras nos totais.
- Peso corporal e medidas.
- Sincronização entre aparelhos e Wear OS.
- Integração com Health Connect.
- Tema claro.
- Publicação na Play Store.

---

## 9. Arquitetura técnica gratuita

| Camada | Tecnologia | Motivo |
|---|---|---|
| Linguagem | Kotlin | Padrão do Android |
| Interface | Jetpack Compose + Material 3 com tema próprio | Animações e desenho livre (anel) |
| Estado | MVVM com `StateFlow` | Tela reage ao banco; simples de testar |
| Banco | Room (SQLite) | Local, offline, reativo com `Flow` |
| Preferências | DataStore | Ajustes pequenos |
| Widget | Jetpack Glance | Widget escrito em Kotlin, mesmo tema |
| Agendamento | AlarmManager + WorkManager | Hora certa vs. tarefas adiáveis |
| Backup | JSON (kotlinx.serialization) via seletor de arquivos do sistema | Sem servidor |
| Injeção de dependências | Manual (um contêiner) | Menos peças; app pequeno |
| Distribuição | APK gerado por GitHub Actions a cada mudança | Gratuito para repositório público |

Sem Firebase, Supabase, APIs de IA, login ou qualquer serviço pago. O app não pede
permissão de internet.

A lógica (cálculo, regras de notificação, consistência, conquistas, relatórios) fica em
Kotlin puro, sem depender do Android, para poder ser testada em segundos.

**Arredondamento [Decisão]:** os valores são guardados com precisão decimal por 100 g.
Somam-se os valores exatos e arredonda-se só na exibição: calorias para inteiro, macros
para uma casa decimal (inteiro nos cartões resumidos), água para uma casa em litros.
Arredondar só no fim evita que a soma dos itens arredondados divirja do total.

---

## 10. Plano de implementação e testes

| Etapa | Entrega | Verificação |
|---|---|---|
| 1. Estratégia | Este documento | — |
| 2. Design | Três direções, escolha, design system | Contraste WCAG AA, alvos de toque ≥ 48 dp |
| 3. Protótipo | Telas principais com dados de exemplo | Imagens geradas das telas reais |
| 4. MVP | Itens 1 a 9 do escopo | Testes automatizados abaixo |
| 5. Gamificação | Item 10 do escopo | Testes de regras |
| 6. Entrega | APK instalável | Compilação, testes e lint sem erros |

**Testes automatizados**

- Cálculo de calorias e macros; porções parciais; alteração de quantidades; arredondamento.
- Registro duplicado; desfazer; refeição fora do plano.
- Mudança de data (virada do dia; cada dia tem seus registros).
- Persistência no banco (Room em memória).
- Estado do widget a partir dos dados.
- Regras de notificação (não lembrar o registrado, silêncio, teto, ritmo de água).
- Backup e restauração (ida e volta sem perda).
- Consistência, conquistas, retomada e relatórios.
- O funcionamento offline é garantido por construção: o app não declara permissão de internet.

**O que não dá para testar neste ambiente:** comportamento em aparelho real (alarmes sob
economia de bateria de cada fabricante, atraso de widget, vibração). Esses pontos têm
uma lista de verificação manual em [04-CHECKLIST-APARELHO.md](04-CHECKLIST-APARELHO.md).

---

## Pontos que pedem validação profissional

- **Nutricionista:** adequação do plano cadastrado, metas de água, e se a regra "meta =
  soma do plano" atende ao acompanhamento desejado.
- **Psicólogo:** texto das mensagens de retomada e do motivo opcional do dia difícil;
  limites do app para quem tem histórico de transtorno alimentar.
- **Dados nutricionais:** a TACO traz valores médios de alimentos; produtos de marca
  devem ser cadastrados pelo rótulo.

## Referências citadas

- Burke LE, Wang J, Sevick MA. Self-monitoring in weight loss: a systematic review. *J Am Diet Assoc*, 2011.
- Gollwitzer PM, Sheeran P. Implementation intentions and goal achievement: a meta-analysis. *Adv Exp Soc Psychol*, 2006.
- Lally P et al. How are habits formed: modelling habit formation in the real world. *Eur J Soc Psychol*, 2010.
- Kluger AN, DeNisi A. The effects of feedback interventions on performance. *Psychol Bull*, 1996.
- Marlatt GA, Gordon JR. *Relapse Prevention*. Guilford, 1985.
- Polivy J, Herman CP. Dieting and binging: a causal analysis. *Am Psychol*, 1985.
- Ryan RM, Deci EL. Self-determination theory and the facilitation of intrinsic motivation. *Am Psychol*, 2000.
- Schultz W, Dayan P, Montague PR. A neural substrate of prediction and reward. *Science*, 1997.
- NEPA/UNICAMP. *Tabela Brasileira de Composição de Alimentos — TACO*, 4ª ed., 2011.
