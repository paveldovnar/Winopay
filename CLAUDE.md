claude.md — WinoPay (Claude Code Rules)

0) Основное

Ты пишешь только промпты и инструкции для Claude Code. Код пишет Claude Code.
В каждом ответе Claude должен действовать как инженер, который обязан довести фикс до проверяемого результата.

⸻

1) Режим работы: FIX-first, не “аудиты ради аудитов”

По умолчанию: FIX + VERIFY.
Аудит делай только если без него нельзя найти root cause, и сразу после аудита — прямой фикс.

Запрещено:
	•	“Everything complete” без доказательств.
	•	Подменять “фикс” описанием.
	•	Заменять проблему другой проблемой.

Разрешено:
	•	Делить задачу на: Root cause → Patch → Proof (logs/tests/manual).

⸻

2) Multichain-ready — железное правило №1

Любое изменение должно сохранять архитектуру готовой к мультичейн (Solana / EVM / Tron и т.д.).

2.1. Где должен жить чейн-специфик код
	•	UI/Pos flow/worker orchestration не должны импортировать solana.* и вообще быть привязаны к конкретной сети.
	•	Чейн-логика должна быть за интерфейсами из payments/:
	•	PaymentRail + PaymentRailFactory
	•	TokenPolicyProvider (или аналог)
	•	(если нужно) generic failover / RPC abstractions

2.2. Данные и инвойсы

Invoice хранит как минимум:
	•	railId (solana/evm/tron)
	•	networkId (devnet/mainnet-beta/1/56/…)
	•	token identifiers в виде chain-native id (Solana mint / EVM contract / Tron contract)
	•	txId (signature/hash) и поля для аудит-трейла

Если добавляется поле в InvoiceEntity → обязательна миграция + тест миграции (androidTest или room-testing).

⸻

3) Multi-stablecoin policy — железное правило №2

Если на выбранной сети доступны USDC и USDT, то:

3.1. Acceptance rule (1:1 stablecoin equivalence)

Если мерчант “ждёт USDT”, но пользователь отправил USDC (или наоборот) — оплата засчитывается, если:
	•	получатель корректный
	•	amount ≥ expected (с допустимой tolerance)
	•	токен ∈ allowlist для этой сети

3.2. UX/audit trail
	•	В инвойс сохраняем actualTokenUsed (какой реально пришёл)
	•	Если пришёл не “primary” токен → сохраняем paymentWarningCode (например PAID_WITH_DIFFERENT_STABLECOIN)
	•	В UI потом можно показывать предупреждение, но ядро должно уметь сохранять это уже сейчас.

3.3. Где живёт allowlist

Allowlist и правила equivalence живут в token policy (chain-agnostic интерфейс) и конкретных реализациях (Solana/EVM/Tron).
Запрещено жёстко хардкодить “если USDC → разреши USDT” внутри низкоуровневого валидатора без policy слоя.

⸻

4) Детект платежа: “amount-only” + multi-strategy fallback

Мы не полагаемся на ref/memo/tag. Валидация только по:
	•	recipient (или token account/контракт событие)
	•	amount (merchant-protect rounding)
	•	token allowlist
	•	time gate (createdAt / blockTime / blockNumber)
	•	network/rail match

4.1. Fallback обязателен

Если основной способ не видит tx, должен включаться fallback:
	•	Solana: derived ATA → discover token accounts → (опционально) wallet-level scan / signature scan
	•	EVM: logs scan (Transfer) по контрактам allowlist
	•	Tron: events scan по allowlist контрактам

Важно: fallback должен быть симметричен в foreground и worker.

4.2. Логи как доказательство

Добавляй “one-line logs” (grep-friendly) с:
	•	invoiceId, railId, networkId, currency/token
	•	polling targets + strategy
	•	provider used (rpc)
	•	выбранная signature/txId
	•	gate decision (PASS/FAIL) + причины

⸻

5) Soft-expire: UI решает, worker не auto-expire

Правило:
	•	По истечении дедлайна показываем soft-expire dialog: “Wait +5 min” / “Revoke”
	•	Пока диалог открыт — детект не останавливается
	•	Worker не имеет права сам ставить EXPIRED. Только UI/PosManager через явный revokeInvoice().

Дедлайн хранится в DB (deadlineAt) и worker читает его каждый poll.

⸻

6) Single active invoice: “невозможно выйти без Cancel/Success”
	•	BackHandler должен перехватывать выход из активной оплаты (QR/Pending/Soft-expire)
	•	Нельзя создать новый invoice, если активный существует
	•	Устранить race window: атомарность через Mutex / транзакцию / другой механизм

⸻

7) Баланс на Dashboard: всегда конвертация из USD

Источник истины:
	•	stablecoin balance считается как (USDC+USDT) = USD
	•	затем: USD → selected currency
UI:
	•	большая строка: X,XXX.XX <selected currency>
	•	мелко: = X,XXX.XX USD

Нельзя “подписывать” USD баланс выбранной валютой без конвертации.

⸻

8) Build flavors: devnet/mainnet

Любые значения mints/RPC/cluster берём из BuildConfig per-flavor.
Нельзя допускать “devnet хардкод в mainnet” и наоборот.

⸻

9) Требования к PR/ответу Claude (обязательно)

Каждый ответ Claude должен содержать:
	1.	Root cause (1–3 абзаца)
	2.	Files changed (список)
	3.	How to verify:
	•	команды сборки
	•	тесты
	•	logcat filters
	•	manual сценарий (по шагам)
	4.	Risks/regressions (коротко)

Запрещено говорить “tests pass”, если их реально нельзя запустить или они не запускались.

⸻

10) Никаких сюрпризов

Если что-то “не получается” из-за лимитов/окружения/Android-only classes:
	•	предложи альтернативу: androidTest вместо unitTest, мок/сейм, или перенос логики в pure Kotlin модуль.
	•	но НЕ оставляй задачу “как есть”.