# Tradutor Instantâneo (Português • Inglês • Espanhol)

Aplicativo de tradução rápida para **Desktop (Windows e Linux)** e **Mobile (Android)** desenvolvido em **Java**, seguindo o padrão visual e de interação do **Google Tradutor** (abas de seleção rápida de idiomas, detecção automática do idioma de origem, botão central de inversão `⇄` e tradução instantânea em tempo real conforme você digita).

Projeto criado para exercitar programação orientada a objetos em Java, requisições HTTP assíncronas com debounce (`Timer` + `SwingWorker`) e compartilhamento das classes de serviço entre o app Desktop e o pacote nativo Android (`.apk`).

---

## Principais Funcionalidades

- **Interação estilo Google Tradutor:** basta começar a digitar no painel da esquerda e a tradução aparece sozinha no painel da direita em tempo real, sem precisar ficar clicando em botões a cada frase.
- **Abas de Idioma + Detecção Automática:** alterne com um clique entre **Detectar idioma**, **Português**, **Inglês** e **Espanhol**.
- **Inversão Instantânea (`⇄`):** troca os idiomas de origem e destino invertendo automaticamente o texto já traduzido.
- **Modo Offline Integrado:** conta com um dicionário embutido de frases e expressões do dia a dia caso o computador ou celular esteja sem internet.
- **Multiplataforma (Windows, Linux e Android APK):** pacotes executáveis prontos na pasta `dist/` e na aba **Releases**.

---

## Estrutura do Projeto

```text
├── compilar_e_executar.bat         # Compila e abre a versão Desktop no Windows
├── run_linux.sh                    # Compila e abre a versão Desktop no Linux
├── SlavicPolyglot.exe              # Executável direto para Windows
├── dist/
│   ├── SlavicPolyglot.exe          # Executável Windows
│   ├── SlavicPolyglot.jar          # Pacote JAR multiplataforma (Windows / Linux)
│   ├── SlavicPolyglot-linux.run    # Binário auto-executável para Linux
│   └── SlavicPolyglot-mobile.apk   # Instalador Android (para enviar e instalar no celular)
├── src/
│   └── com/gabrila7/slavicpolyglot/
│       ├── Main.java                       # Ponto de entrada Desktop
│       ├── model/
│       │   ├── Language.java               # Idiomas suportados (Auto, PT, EN, ES)
│       │   └── TranslationEntry.java       # Modelo do resultado da tradução
│       ├── service/
│       │   ├── OfflineDictionary.java      # Dicionário local e detector de idioma
│       │   └── TranslationService.java     # Cliente de tradução neural + fallback
│       └── ui/
│           └── MainFrame.java              # Interface inspirada no Google Tradutor
└── mobile-android/
    └── src/main/
        ├── AndroidManifest.xml
        └── java/com/gabrila7/slavicpolyglot/mobile/
            └── MainActivity.java           # Interface Android estilo Google Translate App
```

---

## Como Executar

### No Windows
- Dê dois cliques em `SlavicPolyglot.exe` ou em `compilar_e_executar.bat`.

### No Linux
```bash
chmod +x dist/SlavicPolyglot-linux.run
./dist/SlavicPolyglot-linux.run
```

### No Celular (Android)
Envie o arquivo `dist/SlavicPolyglot-mobile.apk` (ou baixe na aba **Releases** do repositório) para o celular e toque nele para instalar.

---

## Autor

Gabriel Vinícius  
Estudante de Ciência da Computação  
GitHub: [@Gabrila7](https://github.com/Gabrila7)
