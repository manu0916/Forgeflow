# Relatório Técnico e de Game Design — ForjaNexus (Android APK)

## 1. Identificação do Projeto e Arquivo Instalável (APK)

- **Nome do Jogo:** ForjaNexus
- **Pacote Android (`applicationId`):** `com.aistudio.forjanexus.kxpqmr`
- **Arquivos APK Gerados no Projeto:**
  - `ForjaNexus-debug.apk` (na raiz do projeto, pronto para download e instalação direta em Android)
  - `app/build/outputs/apk/debug/app-debug.apk`
- **Como Baixar / Exportar no Google AI Studio:**
  - No menu superior/configurações do Google AI Studio, selecione **Exportar ZIP** ou **Gerar APK/AAB**, ou baixe diretamente o arquivo **`ForjaNexus-debug.apk`** pelo explorador de arquivos na raiz do projeto.
- **Como Recompilar Localmente (Android Studio / Linha de Comando):**
  1. Extraia o projeto e abra-o no **Android Studio** (compatível com SDK 24 a 36).
  2. Ou execute via terminal na raiz do projeto:
     ```bash
     gradle :app:assembleDebug
     ```
  3. Para executar a suíte de testes automatizados (Unitários + Robolectric):
     ```bash
     gradle :app:testDebugUnitTest
     ```

---

## 2. Arquitetura Técnica e Engine

- **Linguagem e UI:** 100% **Kotlin** com **Jetpack Compose** e renderizador gráfico dedicado 2.5D em `Canvas` acelerado por hardware (`FactoryCanvas25D.kt`).
- **Padrão Arquitetural:** **MVVM (Model-View-ViewModel)** determinístico com `StateFlow` imutável e loop de simulação assíncrono em Coroutines (`10 ticks/segundo`, com multiplicadores de velocidade `1x`, `2x` e `3x` + pausa instantânea).
- **Persistência Offline:** Sistema completo em `SaveManager.kt` utilizando armazenamento local JSON com salvamento automático periódico (a cada 15 segundos), salvamento manual em 1 toque e recuperação automática contra dados corrompidos.
- **Áudio Procedural Industrial:** Sintetizador de áudio em tempo real (`SoundEngine.kt`) via `AudioTrack` PCM 16-bit, gerando efeitos sonoros industriais (construção metálica, demolição, caixa registradora nas vendas, conclusão de pesquisa e trilha ambiente sintética) sem depender de arquivos externos pesados.

---

## 3. Sistemas de Jogo Implementados

### 3.1. Câmera Tátil 2.5D e Construção Mobile
- **Controles por Gestos:** Arrasto livre com 1 dedo (pan), pinça suave com 2 dedos (zoom de `0.55x` a `2.2x`) e botões de atalho rápido no HUD para saltar entre os 4 biomas.
- **Previsualização Fantasma (Ghost Preview):** Ao escolher uma máquina, o primeiro toque posiciona a prévia verde/vermelha com seta de direção e raio elétrico; o segundo toque (ou o botão **"Construir Aqui"**) confirma a obra. Esteiras e Postes Condutores têm colocação instantânea e sugerem automaticamente o bloco seguinte na direção do fluxo.
- **Ferramentas de Edição:** Rotação em 4 direções (`Norte`, `Leste`, `Sul`, `Oeste`), histórico de **Desfazer (Undo)** de até 20 ações e modo **Remover/Demolir** com reembolso de 80% do custo + venda automática do estoque interno.

### 3.2. Mapa Multi-Biomas (`34 x 24` blocos)
1. **Vale Esmeralda (Noroeste — Inicial):** Clima estável, sem penalidades; rico em Ferro, Cobre, Madeira e Pedra.
2. **Dunas de Silício (Nordeste — Deserto):** Rico em Ouro e Quartzo de Silício; oferece **+50% de geração em Painéis Solares** (`60 kW`), mas tempestades de areia reduzem o ritmo térmico em 20% e encarecem obras até pesquisar *Expansão nas Dunas & Blindagem Térmica*.
3. **Tundra Boreal (Sudoeste — Ártico):** Depósitos de Cristal de Cobalto e Ouro; o frio acelera máquinas térmicas em **+20%**, mas reduz esteiras comuns MK1 em 20% até desbloquear *Operações Criogênicas* e Esteiras MK2.
4. **Caldeira Magmática (Sudeste — Vulcânico):** Fendas Magmáticas exclusivas para Usinas Geotérmicas de **220 kW** e veios puros de Ouro, Cobalto e Pedra.

### 3.3. Sistema de Energia (`PowerSystem.kt`) e Overlay Visual da Malha
- **Geração:**
  - **Dínamo a Biomassa:** `50 kW` contínuos (`75 kW` quando abastecido com Madeira ou Tábuas), raio de 5 blocos.
  - **Painel Solar Industrial:** `40 kW` (`60 kW` nas Dunas de Silício), raio de 4 blocos.
  - **Usina Geotérmica:** `160 kW` (`220 kW` sobre Fenda Magmática), raio de 7 blocos.
  - **Poste Condutor:** Retransmite a rede elétrica via busca em largura (BFS) por 6 blocos (`8 blocos` com pesquisa solar).
- **Consumo e Estado `'Offline'`:**
  - Todas as máquinas produtivas exigem energia (`10 kW` a `48 kW`).
  - Quando a demanda supera a geração ou uma máquina fica sem conexão com um gerador/poste, ela entra imediatamente no estado **`OFFLINE`** (`building.isOffline = true`), pausando a produção e exibindo alertas em tempo real no mapa, no HUD (`• X OFFLINE`), no Inspetor e na Central da Rede Elétrica.
- **Overlay Visual da Malha Elétrica (`Malha: ON / OFF`):**
  - **Verde Esmeralda (`#22C55E`):** Máquinas conectadas e energizadas (`⚡ -XXkW`) e geradores ativos (`+XXkW`), com pulsos luminosos fluindo pelos cabos.
  - **Âmbar/Laranja (`#F59E0B`):** Máquinas conectadas à rede, mas paralisadas por falta de potência total (`FALTA kW`).
  - **Vermelho Carmesim (`#EF4444`):** Máquinas e postes isolados fora do alcance da rede elétrica (`SEM REDE` / `OFF`).

### 3.4. Cadeias Produtivas e Economia Coerente
Cada etapa produtiva multiplica o valor de venda das matérias-primas:
- **Matérias-Primas (Extratoras MK1 / MK2):**
  - Toras de Madeira (`$3`), Pedra Bruta (`$3`), Minério de Ferro (`$4`), Minério de Cobre (`$5`), Quartzo de Silício (`$10`), Minério de Ouro (`$12`), Cristal de Cobalto (`$16`).
- **Refino Primário (Fundição Térmica & Serraria Industrial):**
  - Tábuas Tratadas (`$9`), Bloco de Pedra (`$9`), Barra de Ferro (`$12`), Barra de Cobre (`$14`), Pastilha de Silício (`$28`), Lingote de Ouro (`$34`), Liga Criogênica (`$46`).
- **Corte e Conformação (Prensa & Cortadora):**
  - Caixa de Transporte (`$24`), Tijolo Refratário (`$24`), Chapa de Ferro (`$26`), Fio de Cobre (`$28`), Engrenagem (`$30`), Contato de Ouro (`$74`).
- **Montagem Multi-Insumos (Montadora Dupla — 2 ingredientes):**
  - Cabo Isolado (`$62`), Componente Mecânico (`$82`), Circuito Elétrico (`$95`), Estrutura Pesada (`$125`).
- **Alta Tecnologia (Fábrica Nexus — 3 ingredientes):**
  - Circuito Avançado (`$260`), Módulo Industrial (`$340`), Processador Criogênico (`$580`), Núcleo Nexus (`$1.400`).

---

## 4. Verificação e Testes Executados

| Suíte de Teste | Caso de Teste | Resultado |
| :--- | :--- | :--- |
| **UnitTest** | `verify production chains and coherent pricing` — Valida que todas as receitas geram produtos com valor de venda superior à soma dos insumos. | **APROVADO** |
| **UnitTest** | `verify all four biomes and required resources exist on world map` — Confirma a geração dos 4 biomas e de todos os 7 recursos naturais + fendas magmáticas. | **APROVADO** |
| **UnitTest** | `verify PowerSystem tracks generator production machine consumption and offline state` — Valida cálculo de geração, consumo, prioridade de alocação e transição para `OFFLINE`. | **APROVADO** |
| **Robolectric** | `menu opens and starts new game and renders 1080p canvas without crashing` — Testa abertura do menu, início de nova partida, renderização nativa em bitmap `1920x1080` e overlay da malha elétrica. | **APROVADO** |
| **Robolectric** | `full factory simulation pipeline extraction belt smelter power and save load` — Testa extração, esteiras, venda automática, fundição, divisor/junção, árvore de pesquisa, queda de energia (`OFFLINE`) e ciclo de Save/Load. | **APROVADO** |
