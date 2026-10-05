# ForjaNexus — Jogo Mobile 2.5D de Fábrica e Automação (Android)

Jogo completo de construção e automação industrial 2.5D para Android, desenvolvido em **Kotlin + Jetpack Compose** com renderização acelerada em `Canvas`.

## 📲 Download Direto do APK Instalável

O instalador Android pronto para celular está incluído diretamente na raiz deste repositório:

- **[📥 Baixar `ForjaNexus-debug.apk`](./ForjaNexus-debug.apk)** *(clique em "View raw" ou "Download raw file" no GitHub para baixar no celular)*
- **[📄 Ler o Relatório Técnico Completo (`RELATORIO_TECNICO_FORJANEXUS.md`)](./RELATORIO_TECNICO_FORJANEXUS.md)**

---

## ⚙️ Principais Funcionalidades

- **Visão 2.5D em Pixel Art Industrial:** Câmera ortográfica livre controlada por gestos (arrasto com 1 dedo para pan e pinça com 2 dedos para zoom de `0.55x` a `2.2x`).
- **4 Biomas Distintos (`34 x 24` blocos):**
  - **Vale Esmeralda:** Clima estável com depósitos de Ferro, Cobre, Madeira e Pedra.
  - **Dunas de Silício:** Ouro e Quartzo de Silício, com **+50% de geração solar** e tempestades de areia.
  - **Tundra Boreal:** Cristal de Cobalto e Ouro, com **+20% de resfriamento térmico** e nevascas.
  - **Caldeira Magmática:** Fendas Magmáticas para Usinas Geotérmicas de **220 kW**.
- **Sistema de Energia (`PowerSystem`) e Overlay Visual da Malha:**
  - Geradores (**Dínamo a Biomassa**, **Painel Solar Industrial** e **Usina Geotérmica**) e **Postes Condutores**.
  - Máquinas sem energia suficiente entram automaticamente em estado **`OFFLINE`**.
  - Overlay visual colorido em tempo real (**Verde** = Energizada, **Âmbar** = Falta kW / Sobrecarga, **Vermelho** = Sem Rede / Offline).
- **Cadeia Produtiva de 4 Estágios (20 Receitas):**
  - Extração (`Extratora MK1 / MK2`) → Logística (`Esteiras MK1 / MK2`, `Divisor`, `Junção`, `Filtro Seletor`) → Refino (`Fundição Térmica`, `Serraria`) → Conformação (`Prensa & Cortadora`) → Montagem (`Montadora Dupla`, `Fábrica Nexus`).
- **100% Offline:** Salvamento automático e manual em JSON + sintetizador de áudio industrial procedural.

---

## 🛠️ Como Compilar Localmente

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```
