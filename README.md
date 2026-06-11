# 🏰 Cashtelo

<div align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" alt="Cashtelo Logo" width="120" height="120" />
  <br/>
  <strong>A sua fortaleza financeira pessoal no Android.</strong>
</div>

<br/>

## 🎯 Visão Geral do Produto
O **Cashtelo** é um aplicativo móvel de gestão financeira pessoal desenhado para funcionar como uma "fortaleza financeira". O objetivo principal é fornecer aos usuários um controle total sobre suas receitas, despesas, carteiras e categorias através de uma interface fluida, segura e baseada em operações CRUD rigorosas.

Todo o aplicativo foi projetado com foco absoluto em **Experiência do Usuário (UX)** e **Design de Interface (UI)**, incorporando transições suaves, feedback tátil, layouts em "Glassmorphism" (efeito vidro) e micro-animações, entregando um produto de aspecto premium.

---

## 🛠️ Stack Tecnológico e Arquitetura

O Cashtelo é um aplicativo Android Nativo, desenvolvido com os padrões mais modernos recomendados pela Google.

### 📱 Frontend (Interface & Interação)
* **Linguagem:** [Kotlin](https://kotlinlang.org/)
* **Arquitetura Base:** Single-Activity Architecture via **Navigation Component** (Jetpack Navigation).
* **Design de Interface:** XML com **ConstraintLayout** e componentes do **Material Design 3**.
* **Animações e Feedback:** Amplo uso de `ObjectAnimator`, `ValueAnimator` e Interpoladores físicos (`OvershootInterpolator`, `DecelerateInterpolator`) para criar UI responsiva. Integração com a classe `Vibrator` para micro-feedbacks hápticos.
* **Sistema de Ícones:** **Adaptive Icons** 100% vetoriais (XML) criados do zero sem perda de resolução.

### 🧠 Lógica e Gerenciamento de Estado
* **Padrão Arquitetural:** **MVVM** (Model - View - ViewModel) para separação clara de responsabilidades.
* **Ciclo de Vida:** Uso de `LiveData` e `ViewModel` para reter o estado da UI entre as transições de tela e rotações.
* **Corrotinas:** `Kotlin Coroutines` (com `viewModelScope` e `Dispatchers.IO`) para garantir que todas as transações de banco de dados e compressões de imagem aconteçam de forma assíncrona, mantendo a tela a 60 frames por segundo.

### 🗄️ Backend Local e Banco de Dados
O aplicativo funciona de forma *offline-first*, garantindo máxima privacidade:
* **Banco de Dados:** **Room Database** (Camada de abstração oficial do Android para SQLite).
* **Entidades:** Models como `Transaction` (para lançamentos financeiros) e `User` (para configurações locais de perfil).
* **Migrações:** Histórico controlado de versões (`MIGRATION_1_2`, `MIGRATION_3_4`) para atualização do Schema sem perda de dados do usuário.

### 🔒 Segurança e Integração do Sistema
* **Biometria Nativa:** Integração oficial com a biblioteca `androidx.biometric`, suportando Face ID e Leitura de Digital para bloqueio/desbloqueio das configurações.
* **Upload e Gestão de Imagens:**
  * Solicitação de arquivos via API moderna `ActivityResultContracts.GetContent()`, dispensando a necessidade de permissões intrusivas (`READ_EXTERNAL_STORAGE`).
  * Processamento de `Bitmap` para redimensionamento, compressão otimizada (JPEG) e salvamento invisível no espaço privado do App (`context.filesDir`).
  * Persistência segura gravando apenas o `URI` no banco de dados, prevenindo falhas clássicas de *CursorWindowAllocation* (não armazenando BLOBs no SQLite).

---

## 🚀 Principais Funcionalidades

1. **Dashboard (Home):**
   * Resumo de Saldo, Receitas e Despesas formatado dinamicamente para o padrão Monetário Local (`NumberFormat` do Java).
   * Contador do "Saldo Animado" (os números sobem em roleta ao abrir o App).
   * Avatar Dinâmico e "Mascote das Finanças" que muda de humor baseado no saldo do usuário.

2. **Gestão de Transações:**
   * Inserção, visualização e listagem em operações CRUD completas usando MVVM.

3. **Perfil do Usuário:**
   * Troca de nome reflete simultaneamente em todas as telas em tempo real (Graças ao Observer do LiveData).
   * Personalização da foto do perfil (Carregamento nativo com scale e fallbacks automáticos para iniciais do nome se a foto não existir).
   * Verificador Visual de Força da Senha (com `TextWatcher`).
   * Switch de bloqueio biométrico (requer confirmação real do sistema Android ao ser ativado).

---

## 💻 Como Rodar o Projeto (Desenvolvedores)

1. Clone o Repositório: `git clone https://github.com/SeuUsuario/Cashtelo.git`
2. Abra o projeto no **Android Studio** (Recomendado versão Flamingo ou superior).
3. Aguarde a sincronização do **Gradle**.
4. Clique no botão de `▶ Run` na barra superior tendo um Emulador ou Smartphone físico selecionado.
5. *(Opcional)* Gere um instalador acessando no menu superior: `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`.

---

## 👥 Desenvolvedores e Créditos

Projeto acadêmico desenvolvido para a matéria de **Linguagem de Programação Mobile**.

* **Kauã Oliveira** - [Kauaog13 no GitHub](https://github.com/Kauaog13)
* **Victoria Ferreira** - [victoriafe-sa no GitHub](https://github.com/victoriafe-sa)

---

<div align="center">
  <i>Construído em Kotlin. Uma fortaleza inquebrável para seu controle financeiro.</i>
</div>