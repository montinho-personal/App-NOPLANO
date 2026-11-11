# Contexto do projeto (para o Claude)

**No Plano**: app Android nativo, pessoal, de adesão a uma dieta já definida. Um único
usuário, custo de infraestrutura zero, tudo offline. O dono não programa: seja didático,
faça mudanças pequenas e seguras, e rode os testes antes de concluir.

Stack: Kotlin, Jetpack Compose (Material 3 com tema próprio), Room, DataStore, Glance
(widgets), AlarmManager + WorkManager, kotlinx.serialization. Sem Hilt: as dependências
ficam em `NoPlanoApp`.

## Antes de concluir qualquer mudança

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Se mexeu em tela, gere de novo as imagens e olhe o resultado:

```bash
./gradlew :app:recordRoborazziDebug
```

## Regras inegociáveis

- **Sem internet.** Não adicionar a permissão `INTERNET`, Firebase, Supabase, APIs de IA
  ou qualquer serviço pago ou com login.
- **Não inventar valores nutricionais.** Alimentos vêm da TACO (`assets/taco.json`) ou do
  rótulo, digitado pelo usuário. Todo alimento guarda a `fonte`.
- **Arredondar só na exibição** (`domain/Nutri.kt`, objeto `Fmt`). Somar sempre valores exatos.
- **Um registro por refeição do plano por dia** (índice único em `Registro`). Tocar duas
  vezes não duplica.
- **Registro guarda os valores do momento** (`ItemRegistro`). Editar o plano depois não
  altera o histórico. `DiaResumo` guarda a meta daquele dia.
- **Gamificação saudável:** nada premia comer menos que a meta; nenhum contador zera;
  ausência de registro nunca é tratada como "não comeu"; sem cor de alerta para "passou da meta".
- **A meta do dia é a soma do plano** (opção principal de cada refeição). O app não calcula metas.
  O **alvo de macros** (`Plano.alvo*`) é opcional e sempre digitado pelo usuário; serve só para
  comparar o plano enquanto ele é editado. Não substitui a meta do dia na tela Hoje.
- **Toda gravação passa pelo `Repositorio`**, que chama `aoMudar` (atualiza widgets e
  reagenda lembretes). Não gravar direto nos DAOs a partir da interface.
- **Mudou o banco?** Suba a `version` em `data/Banco.kt`, escreva a migração em `MIGRACOES`
  e um teste como `MigracaoTest`. Nunca usar migração destrutiva: os dados do usuário só
  existem no aparelho dele. Os esquemas de cada versão ficam em `app/schemas`.
- **Mudou o formato do backup?** Suba `versao` em `data/Backup.kt` e continue lendo a anterior.

## Onde fica cada coisa

- `domain/`: Kotlin puro, sem Android, coberto por testes. Cálculo (`Nutri`), alvo e passos de
  ajuste (`Alvo`), estado do dia
  (`Dia`), regras de lembrete (`Lembretes`), consistência e conquistas (`Consistencia`),
  relatórios e padrões (`Relatorios`), frases (`Mensagens`), preferências (`Ajustes`).
- `data/`: entidades e DAOs do Room, `Repositorio`, `Preferencias`, `Backup`, `Taco`, `Exemplo`.
- `ui/`: `Tema.kt` (cores e tipografia; a cor de destaque é `Cores.destaque`),
  `Componentes.kt`, e uma pasta por tela (`hoje`, `plano`, `progresso`, `ajustes`).
  Cada tela tem um `...Conteudo` sem estado, usado também para gerar as imagens.
- `widget/Widgets.kt`: widgets compacto e expandido. A cor do widget é definida ali também.
- `notif/Notificacoes.kt`: canais, agendamento (um alarme por vez), ações da notificação.
- `app/src/test`: testes. `TelasTest` gera as imagens de `docs/telas`.

## A dieta do usuário fica em `app/src/main/assets/plano.json`

O dono pede mudanças na dieta por mensagem ("troca o arroz por 120 g de batata doce na
refeição 2"). Para fazer:

1. Edite `plano.json`. Alimentos: código da TACO (`"taco": 3`, procure em `assets/taco.json`)
   ou um alimento de rótulo declarado em `"alimentos"` (valores da porção do rótulo; nunca
   invente números, peça o rótulo se não tiver).
2. **Suba `"versao"`** e escreva em `"notas"` o que mudou, em uma frase. Sem subir a versão,
   o celular não oferece a mudança.
3. Rode os testes (`PlanoArquivoTest` confere que o arquivo é válido) e faça push na `main`.
4. O GitHub gera e publica o APK; o celular atualiza pelo Obtainium e o app pergunta se
   aplica o plano novo. O histórico de registros não muda.

O app aplica sozinho só no primeiro uso (sem nenhuma refeição cadastrada). Planos que não
estão no arquivo ficam como estão; os que estão (pelo nome) têm as refeições substituídas.

## Identidade visual

Direção "Pulso": fundo preto, uma única cor de destaque (amarelo vivo `#FFD60A`), números
grandes em Barlow Condensed itálico, rótulos em caixa alta, formas retas e segmentos
inclinados. Detalhes em `docs/02-DESIGN.md`. Não acrescentar cores.

## Publicação

O GitHub Actions (`.github/workflows/apk.yml`) roda testes e gera o APK a cada mudança na
`main`. Ele só publica uma versão em Releases quando existem os segredos
`NOPLANO_KEYSTORE_B64` e `NOPLANO_KEYSTORE_PASSWORD` (chave de assinatura fixa). Sem a
mesma chave, uma versão nova não instala por cima da anterior.
