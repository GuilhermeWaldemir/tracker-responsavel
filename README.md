# Tracker Anti-Vagabundo

App para Windows que fica rodando em segundo plano e, toda vez que você abre um jogo ou launcher (Steam, Epic Games...), pergunta se você já terminou suas responsabilidades — mostrando quantas horas você já jogou na semana.

```
┌───────────────────────── Tracker Anti-Vagabundo ─────────────────────────┐
│  Você abriu steam.exe.                                                    │
│  Nesta semana você já jogou 7h 45min.                                     │
│  Já terminou todas as suas responsabilidades? Vai jogar mesmo?            │
│                                                                           │
│          [ Vou jogar mesmo assim ]   [ Tem razão, fecha o jogo ]          │
└───────────────────────────────────────────────────────────────────────────┘
```

## Funcionalidades

- Detecta automaticamente quando um jogo da sua lista é aberto.
- Janela de aviso por cima do jogo com as horas jogadas na semana; você escolhe jogar ou fechar o jogo.
- Conta o tempo jogado (semana de segunda a domingo) e salva em um banco SQLite local.
- Ícone na bandeja do sistema com o total da semana e a opção de sair.
- Opção de iniciar junto com o Windows.
- Não conta tempo em dobro com vários jogos abertos, nem o tempo em que o PC ficou em suspensão.

## Como rodar

Requisito: **Java 21+**. Não é preciso instalar o Maven — o projeto usa o Maven Wrapper.

```powershell
.\mvnw.cmd package                      # compila e roda os testes
javaw -jar target\tracker-anti-vagabundo.jar
```

(`javaw` roda sem abrir janela de terminal; use `java` para ver os logs.)

### Iniciar com o Windows

Clique com o botão direito no ícone da bandeja e marque **Iniciar com o Windows**. O app copia o `.jar` para `%APPDATA%\TrackerAntiVagabundo\` e se registra em `HKCU\Software\Microsoft\Windows\CurrentVersion\Run` (não precisa de administrador). Desmarque para remover.

A opção só aparece habilitada quando o app roda a partir do `.jar`. Depois de gerar uma versão nova, desmarque e marque de novo para atualizar a cópia instalada.

## Configurando os jogos

Na primeira execução é criado o arquivo `%APPDATA%\TrackerAntiVagabundo\jogos.txt` com uma lista padrão. Adicione um executável por linha:

```
steam.exe
epicgameslauncher.exe
cs2.exe
```

Para descobrir o nome do executável de um jogo: Gerenciador de Tarefas → botão direito no jogo → **Ir para detalhes**. Reinicie o app depois de editar.

Os dados ficam em `%APPDATA%\TrackerAntiVagabundo\tracker.db`.

## Como funciona

```
 a cada 5s
     │
     ▼
ProcessMonitor ──► quais jogos estão abertos? quais acabaram de abrir?
     │
     ├──► PlayTimeTracker ──► SessionRepository (SQLite)
     │        abre/estende/fecha a sessão de jogo
     │
     └──► WarningDialog ──► pergunta e, se pedido, fecha o jogo
```

| Pacote | Responsabilidade |
|---|---|
| `games` | Lista de jogos e detecção de processos (`ProcessHandle` da API do Java) |
| `tracking` | Regras de sessão e cálculo da semana |
| `storage` | Persistência em SQLite via JDBC |
| `ui` | Janela de aviso (Swing) e ícone na bandeja (AWT) |
| `startup` | Inicialização com o Windows via Registro (`reg.exe`) |

Algumas decisões:

- **A sessão é gravada a cada verificação**, não só ao fechar o jogo: se o PC desligar de repente, perde-se no máximo 5 segundos.
- **O tempo conta enquanto *qualquer* jogo estiver aberto**, então Steam + jogo abertos juntos não dobram as horas.
- **Intervalos maiores que 1 minuto entre verificações** (PC em suspensão) não são contados.
- **A leitura de processos é injetada** (`Supplier<Set<String>>`) no `ProcessMonitor`, o que permite testá-lo sem abrir jogos de verdade.

## Testes

```powershell
.\mvnw.cmd test
```

Testes com JUnit 5 cobrem a leitura da lista de jogos, a detecção de jogos abertos, as regras de sessão (incluindo suspensão do PC) e a soma de horas na semana (incluindo sessões que atravessam a virada da semana).

## Tecnologias

Java 21 · Swing/AWT · SQLite (sqlite-jdbc) · Maven · JUnit 5

## Próximos passos

- [x] Iniciar junto com o Windows
- [ ] Impedir que duas cópias do app rodem ao mesmo tempo
- [ ] Meta semanal de horas com aviso ao ultrapassar
- [ ] Histórico por semana e por jogo
