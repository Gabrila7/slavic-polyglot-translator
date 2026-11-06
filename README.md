# SlavicPolyglot — Tradutor EN / ES / RU & Teclado Virtual Russo

Aplicativo em Java (Desktop e Mobile Android) para tradução rápida entre **Inglês**, **Espanhol**, **Russo** e **Português**, contando com um **teclado virtual cirílico (Кириллица)** integrado e sistema automático de transliteração fonética para alfabeto latino.

Projeto desenvolvido para praticar programação orientada a objetos em Java, interfaces gráficas assíncronas com `SwingWorker`, consumo de APIs HTTP sem dependências externas e compartilhamento das classes de negócio (`model` e `service`) entre o aplicativo Desktop (Windows/Linux) e a versão Mobile para Android (`.apk`).

---

## O que o programa faz

- **Tradução entre 4 idiomas:** suporte direto a Inglês (`EN`), Espanhol (`ES`), Russo (`RU`) e Português (`PT`), com atalhos rápidos para inverter pares de idiomas.
- **Teclado Virtual Russo (Cirílico):** permite digitar em russo sem precisar configurar o idioma do teclado no sistema operacional.
  - **Modo Padrão Russo (`ЙЦУКЕН`):** disposição clássica dos teclados russos.
  - **Modo Fonético (`QWERTY -> ЯШЕРТЫ`):** aproxima as letras russas dos sons equivalentes do nosso teclado (ex: `A -> А`, `B -> Б`, `V -> В`, `G -> Г`, `D -> Д`, `O -> О`, `R -> Р`, `S -> С`, `T -> Т`).
- **Transliteração Fonética em tempo real:** exibe abaixo do texto como ler e pronunciar as palavras em alfabeto cirílico usando letras latinas (ex: `Привет друг, как дела?` -> `Privet drug, kak dela?`).
- **Guia de Frases e Fallback Offline:** possui um dicionário interno de expressões frequentes de conversação e viagem que funciona mesmo quando o dispositivo está sem internet.
- **Versão Desktop (Windows e Linux) + Mobile (Android APK):** pacotes prontos na pasta `dist/` e na aba **Releases** para rodar no computador ou instalar diretamente no celular.

---

## Estrutura do Projeto

```text
slavic-polyglot-translator/
├── compilar_e_executar.bat         # Script para compilar e abrir no Windows
├── run_linux.sh                    # Script para compilar e abrir no Linux
├── dist/
│   ├── SlavicPolyglot.exe          # Executável Windows
│   ├── SlavicPolyglot.jar          # Executável multiplataforma (Windows / Linux)
│   ├── SlavicPolyglot-linux.run    # Binário auto-executável para Linux
│   └── SlavicPolyglot-mobile.apk   # Instalador Android (para enviar e instalar no celular)
├── src/
│   └── com/
│       └── gabrila7/
│           └── slavicpolyglot/
│               ├── Main.java                           # Ponto de entrada Desktop (Swing)
│               ├── model/
│               │   ├── KeyboardLayout.java             # Mapas de teclas russas (JCUKEN e Fonético)
│               │   ├── Language.java                   # Enum dos idiomas suportados
│               │   └── TranslationEntry.java           # Registro de tradução e histórico
│               ├── service/
│               │   ├── OfflineDictionary.java          # Dicionário e frases salvas localmente
│               │   ├── TranslationService.java         # Cliente HTTP e fallback offline
│               │   └── TransliterationService.java     # Conversão Cirílico -> Alfabeto Latino
│               └── ui/
│                   ├── MainFrame.java                  # Janela principal Desktop
│                   └── RussianKeyboardPanel.java       # Teclado virtual russo interativo
└── mobile-android/
    └── src/main/
        ├── AndroidManifest.xml                         # Manifesto do app Android
        └── java/com/gabrila7/slavicpolyglot/mobile/
            └── MainActivity.java                       # Interface Mobile nativa em Java
```

---

## Como Executar

### 1. No Windows
- **Pelo Executável:** dê dois cliques em `dist/SlavicPolyglot.exe` (ou em `dist/SlavicPolyglot.jar`) com o Java 11+ instalado.
- **Compilando pelo script:** dê dois cliques em `compilar_e_executar.bat`.

### 2. No Linux
No terminal, dê permissão de execução ao binário ou ao script e rode:

```bash
chmod +x dist/SlavicPolyglot-linux.run
./dist/SlavicPolyglot-linux.run
```
Ou compilando a partir do código-fonte:
```bash
chmod +x run_linux.sh
./run_linux.sh
```

### 3. No Celular (Android)
1. Envie o arquivo `dist/SlavicPolyglot-mobile.apk` (ou baixe na aba **Releases** do repositório) para o celular via WhatsApp, Telegram ou cabo USB.
2. Toque no arquivo `.apk` recebido para instalar (caso o Android peça confirmação de fonte externa, basta permitir a instalação).
3. Abra o app **SlavicPolyglot** no celular.

---

## Autor

Gabriel Vinícius  
Estudante de Ciência da Computação  
GitHub: [@Gabrila7](https://github.com/Gabrila7)
