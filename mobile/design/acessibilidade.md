# Acessibilidade das telas

Metas objetivas do épico (decisão D5 do plano, baseadas no Android e no WCAG AA). Valem para **todos** os quadros.

## Metas
| Item | Meta |
|---|---|
| Contraste de texto | razão ≥ **4,5:1** (texto grande e ícones ≥ 3:1) |
| Alvo de toque | ≥ **48 × 48 dp** |
| Texto base | ≥ **16 sp**; layout que suporta fonte ampliada sem cortar conteúdo essencial |
| Cor | informação **nunca só por cor**: sempre texto e/ou ícone |
| Erros | dizem o que aconteceu e o que fazer; nunca só "erro" |
| Leitura | ordem de leitura lógica registrada por tela; rótulo para leitor de tela em todo elemento sem texto visível |
| Movimento | sem animação essencial para entender a tela |

## Lista de verificação (aplicar a cada quadro)
- [ ] Todo texto atende 4,5:1 sobre o fundo real.
- [ ] Ícones e bordas de campos atendem 3:1.
- [ ] Todo alvo de toque tem pelo menos 48 dp (inclusive ícones pequenos, com área de toque ampliada).
- [ ] O estado de um pedido, de uma regra ou de um envio aparece por texto + ícone.
- [ ] O corpo do texto tem 16 sp ou mais; títulos e legendas não ficam menores que 12 sp.
- [ ] Mensagens de erro dizem o que fazer em seguida.
- [ ] Campos têm rótulo visível (não só texto de exemplo dentro do campo).
- [ ] Ordem de leitura e rótulos de leitor de tela registrados em `INDEX.md`.
- [ ] A tela continua legível com fonte ampliada (ver quadros de fonte ampliada no E4.4).
- [ ] Aviso acadêmico presente; selo SIMULADO onde houver extração, resumo ou comprovante.

## Razões de contraste do catálogo
Calculado em 13/11/2026 sobre os tokens v1.0 (`design/RegulaCheck.pen`, F1 `IkCIw`). Método: luminância relativa sRGB, `(L1+0,05)/(L2+0,05)`, valores sólidos. Meta: texto ≥ 4,5:1; ícone/borda não textual ≥ 3:1.

| Par (primeiro plano / fundo) | Razão | Atende? |
|---|---|---|
| Texto `#112B3C` / branco · neutro `#EFEFEF` · info `#EAF2F8` · erro `#FDEDF0` | 14,64 · 12,73 · 12,93 · 12,93 | Sim |
| Apoio `#526774` / branco · neutro · info · desabilitado `#E9EEF1` | 5,91 · 5,14 · 5,23 · 5,06 | Sim |
| Branco / ação `#205375` · pressionado `#112B3C` | 8,22 · 14,64 | Sim |
| Ação/link `#205375` / branco · neutro | 8,22 · 7,15 | Sim |
| Pendência `#B44809` (ícone) / branco · neutro | 5,42 · 4,72 | Sim |
| Sucesso `#176448` (ícone) / branco · neutro | 7,11 · 6,18 | Sim |
| Erro `#B42335` / branco · neutro · fundo de erro | 6,50 · 5,65 · 5,74 | Sim |
| SIMULADO `#8D3C0C` / `#FFF1E6` | 6,82 | Sim |
| Borda de campo `#82929B` / branco | 3,21 | Sim (não textual) |
| Acento `#F6680E` / branco · neutro | 3,04 · 2,64 | Só decorativo; nunca texto ou ícone isolado |

## Registro da aplicação da lista
*Preencher na tarefa T032:* quadros conferidos, correções feitas e o que não foi conferido.
