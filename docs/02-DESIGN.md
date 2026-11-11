# NO PLANO — Design (Etapa 2)

![Três direções](design/tres-direcoes.png)

## As três direções

| | 1 · Grafite | 2 · Pulso | 3 · Trilha |
|---|---|---|---|
| Ideia | Sofisticação minimalista | Energia esportiva premium | Bem-estar e consistência |
| Fundo | Grafite neutro `#0E1011` | Preto `#090909` | Grafite esverdeado `#111514` |
| Cor de progresso | Esmeralda `#2FD48A` | Lima `#C6F432` | Esmeralda suave `#7FD9AE` + creme |
| Tipografia | Sora (números leves) + Manrope | Barlow Condensed itálico pesado | Fraunces (serifada) + Figtree |
| Elemento-assinatura | Anel do Plano, segmentado por refeição | Número gigante e barra em faixas | Trilha vertical do dia |

## Avaliação

Notas de 1 a 5. São julgamentos de design **[Hipótese]**, não medições.

| Critério | Grafite | Pulso | Trilha |
|---|---|---|---|
| Clareza (responder as 6 perguntas em segundos) | 5 | 4 | 4 |
| Apelo emocional | 4 | 5 | 4 |
| Acessibilidade (contraste, tamanho, leitura) | 5 | 3 | 4 |
| Facilidade de uso com uma mão | 5 | 4 | 3 |
| Motivação sustentável (meses e anos) | 5 | 3 | 5 |
| **Total** | **24** | **19** | **20** |

- **Pulso** empolga no primeiro dia, mas grita. Texto condensado em caixa alta cansa, o
  cartão lima ocupa a tela inteira de atenção e o tom "em jogo" puxa para desempenho,
  que é o oposto de tranquilidade num dia ruim.
- **Trilha** tem a melhor metáfora de continuidade, mas a trilha vertical empurra a água
  para fora da primeira dobra e o botão principal muda de posição conforme o dia avança.
- **Grafite** responde tudo sem rolar, mantém o botão sempre no mesmo lugar e usa a cor
  só onde há progresso. É a que menos envelhece.

## Direção escolhida: Grafite, com a trilha do dia

Fica a base da proposta 1. Da proposta 3 vem a lista de refeições como trilha (pontos
ligados por uma linha), logo abaixo da água, e o tom calmo das mensagens.

## Design system

### Cores

| Papel | Token | Valor | Uso |
|---|---|---|---|
| Fundo | `fundo` | `#0E1011` | Tela |
| Superfície | `superficie` | `#16191B` | Cartões |
| Superfície elevada | `superficieAlta` | `#1D2124` | Chips, campos |
| Linha | `linha` | `#262B2E` | Bordas de 1 dp, trilho vazio |
| Texto | `texto` | `#F2F4F3` | Principal (contraste 17:1 sobre o fundo) |
| Texto secundário | `textoSuave` | `#9AA1A5` | Rótulos (contraste 7:1) |
| Progresso | `esmeralda` | `#2FD48A` | Anel, botão principal, concluído |
| Progresso fraco | `esmeraldaFraca` | `#1F5E44` | Próximo segmento do anel |
| Sobre o progresso | `sobreEsmeralda` | `#06130D` | Texto do botão (contraste 10:1) |
| Carboidratos | `areia` | `#E3C58B` | Barra de carboidratos |
| Gorduras | `lavanda` | `#9AA8E0` | Barra de gorduras |
| Água | `agua` | `#7CCBEB` | Hidratação |
| Erro | `erro` | `#F2857A` | Só falhas do sistema |

Proteína usa a própria esmeralda. Não existe cor de "passou da meta".

### Tipografia

- **Sora** para números e títulos. Peso 300 nos números grandes, 600 nos títulos.
- **Manrope** para texto e rótulos. Pesos 400, 500 e 700.
- Números sempre tabulares, para não "dançar" durante a animação.

| Estilo | Fonte | Tamanho |
|---|---|---|
| Número herói | Sora 300 | 52 sp |
| Título de tela | Sora 600 | 22 sp |
| Título de cartão | Sora 600 | 19 sp |
| Número médio | Sora 400 | 17 sp |
| Corpo | Manrope 400 | 15 sp |
| Rótulo | Manrope 500 | 12 sp |
| Sobrescrito (caixa alta) | Manrope 700, espaçado | 11 sp |
| Botão | Manrope 700, espaçado | 15 sp |

O app respeita o tamanho de fonte do sistema.

### Forma e espaço

- Margem lateral de 22 dp; espaço vertical em múltiplos de 4.
- Cartões com raio de 22 dp e borda de 1 dp; botão principal com raio de 16 dp e 56 dp de altura.
- Alvos de toque de no mínimo 48 dp.

### Movimento

- Anel e barras: 600 ms, curva de desaceleração.
- Números: contagem até o novo valor em 600 ms.
- Conclusão de refeição: o segmento preenche, o botão pulsa uma vez (escala 0,97 → 1).
- Nada pisca nem se repete em laço. Com "remover animações" ligado no sistema, tudo
  aparece direto no estado final.

### Tato

Vibração curta de confirmação ao concluir refeição e ao adicionar água. Pode ser
desligada nos Ajustes.

### Voz

Frases curtas, no presente, sobre o dia. Sem exclamações em série, sem culpa, sem
"você falhou". Exemplos: "Três refeições no plano. A próxima é às 16h." /
"Seu próximo passo é o jantar."
