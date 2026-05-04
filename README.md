# OTP Security Service

Backend-сервис для защиты операций с помощью временных одноразовых кодов (OTP).

## Стек технологий

- Java 17, Spring Boot 3.2, Spring Data JPA, PostgreSQL, BCrypt, JJWT, Angus Mail, jSMPP, Java HTTP Client.

## Запуск

### Требования
- JDK 17+
- Maven 3.8+
- PostgreSQL 17 (создать базу `otp_db`)
- Эмулятор SMPP (например, SMPPsim) для SMS
- Настроенный Telegram бот
- SMTP-сервер для Email

### Настройка
1. Склонируйте репозиторий.
2. Настройте файлы в `src/main/resources`:
    - `application.properties` – подключение к БД, JWT.
    - `email.properties` – учетные данные SMTP.
    - `sms.properties` – параметры SMPP-эмулятора.
    - `telegram.properties` – токен бота и ID чата.
3. Запустите эмулятор SMPP (по желанию).
4. Выполните `mvn spring-boot:run`.

### API Endpoints

#### Аутентификация
- `POST /api/auth/register` – регистрация (роль ADMIN допускается только один раз).
- `POST /api/auth/login` – вход, возвращает JWT.

#### Администратор (требуется токен с ролью ADMIN)
- `PUT /api/admin/otp-config` – изменить длину/время жизни кода.
- `GET /api/admin/users` – список пользователей (кроме админов).
- `DELETE /api/admin/users/{id}` – удалить пользователя и его OTP-коды.

#### Пользователь (требуется токен с ролью USER)
- `POST /api/user/otp/generate` – генерация и рассылка OTP для операции.
- `POST /api/user/otp/validate` – проверка OTP по коду операции.

### Тестирование
Используйте curl. Примеры:
```bash
# Регистрация администратора
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin","role":"ADMIN"}'

# Логин
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
# {"token":"<JWT_TOKEN>", "username":"admin", "role":"ADMIN"}
# Сохраните токен из ответа и используйте в заголовке Authorization: Bearer <token>

# Изменение конфигурации OTP
curl -X PUT http://localhost:8080/api/admin/otp-config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"codeLength":8, "lifetimeSeconds":600}'
# {"id":1,"codeLength":8,"lifetimeSeconds":600}

# Список всех не‑администраторов
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
# [{"id":2,"username":"user1","role":"USER"}]

# Удаление пользователя
curl -X DELETE http://localhost:8080/api/admin/users/<USER_ID> \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
# User and related OTP codes deleted

# Регистрация обычного пользователя
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"user1","role":"USER"}'
# User registered successfully

# Вход под пользователем
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"user1"}'
# {"token":"<USER_TOKEN>", "username":"user1", "role":"USER"}


# Генерация и рассылка OTP
curl -X POST http://localhost:8080/api/user/otp/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -d '{
    "operationId": "<OPERATION_ID>",
    "channels": ["SMS", "EMAIL", "FILE"],
    "email": "user@example.com",
    "phone": "1234567890",
    "telegramChatId": "123456789"
  }'
# OTP generated and sent

# Валидация OTP
curl -X POST http://localhost:8080/api/user/otp/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -d '{"operationId":"<OPERATION_ID>", "code":"<OTP_CODE>"}'
# OTP validated successfully
```

Для использования SMPP можно скачать: https://www.auronsoftware.com/download/
Необходимо запустить сервис активным параметром аутентификации и логином/паролем из properties

Для использования SMTP можно скачать: https://github.com/Nilhcem/FakeSMTP
Здесь ничего прописывать в сервисе не нужно. Просто выполнить curl запросы по шаблонам выше.

### Дополнительная информация
## 1. Конфигурация приложения и сборки (корень проекта)

- **`pom.xml`** — файл сборки Maven. Определяет все зависимости и свойства проекта.
- **`README.md`** — документация.

## 2. Точка входа и базовая конфигурация Spring

- **`OtpServiceApplication.java`** — главный класс, содержит метод `main`.
- **`AppConfig.java`** — конфигурация безопасности. Настраивает `SecurityFilterChain`: отключает CSRF, переводит приложение в stateless-режим, разрешает доступ к эндпоинтам `/api/auth/**` без аутентификации, все остальные запросы требуют авторизации. Добавляет `JwtTokenFilter` перед стандартным фильтром аутентификации. Также предоставляет бин `PasswordEncoder` (BCrypt).
- **`WebConfig.java`** — дополнительная конфигурация веб-слоя.

## 3. Модели данных (Entity)

- **`User.java`** — JPA-сущность таблицы `users`. Хранит логин, хешированный пароль и роль пользователя (`ADMIN` или `USER`).
- **`OtpConfig.java`** — сущность таблицы `otp_config`. Содержит единственную запись с настройками OTP: длина кода (`code_length`) и время жизни в секундах (`lifetime_seconds`). Идентификатор всегда равен 1.
- **`OtpCode.java`** — сущность таблицы `otp_codes`. Содержит сгенерированный код, идентификатор операции, статус (`ACTIVE`, `EXPIRED`, `USED`), время создания, время истечения и ссылку на пользователя-владельца.

## 4. Data Transfer Objects (DTO)

Объекты для приёма и передачи данных через REST API:

- **`RegisterRequest`** — данные для регистрации (логин, пароль, роль). Валидирует, что роль либо `ADMIN`, либо `USER`.
- **`LoginRequest`** — логин и пароль для аутентификации.
- **`LoginResponse`** — возвращается после успешного входа: JWT-токен, имя пользователя и роль.
- **`OtpGenerateRequest`** — запрос на генерацию OTP: идентификатор операции, набор каналов для рассылки (`SMS`, `EMAIL`, `TELEGRAM`, `FILE`), опционально email, телефон, Telegram chat ID.
- **`OtpValidateRequest`** — запрос на проверку кода: идентификатор операции и сам код.
- **`OtpConfigUpdateRequest`** — запрос от администратора для изменения длины кода и/или времени жизни.
- **`UserResponse`** — упрощённое представление пользователя для списка (id, username, role).

## 5. Исключения и обработка ошибок

- **`ServiceException`** — кастомное исключение уровня сервиса.
- **`GlobalExceptionHandler`** — универсальный обработчик исключений с аннотацией `@RestControllerAdvice`.

## 6. Безопасность и аутентификация

- **`JwtTokenProvider`** — создаёт и проверяет JWT-токены. При создании в токен кладётся ID пользователя, имя и роль. Срок действия настраивается в `application.properties`. При проверке извлекает claims.
- **`JwtTokenFilter`** — фильтр, который перехватывает каждый HTTP-запрос, извлекает токен из заголовка `Authorization: Bearer ...`, валидирует его и, если токен корректен, устанавливает аутентификацию в `SecurityContextHolder` с ролью пользователя.
- **`PasswordEncoderUtil`** — утилитарный класс для хеширования паролей с помощью BCrypt.

## 7. Слой доступа к данным (Repository)

- **`UserRepository`** — интерфейс Spring Data JPA для работы с пользователями.
- **`OtpConfigRepository`** — репозиторий для конфигурации OTP.
- **`OtpCodeRepository`** — репозиторий для кодов OTP.

## 8. Слой бизнес-логики (Service)

### Основные сервисы

- **`AuthService`** — обслуживает регистрацию и аутентификацию.
- **`UserService`** — административная работа с пользователями.
- **`OtpService`** — основная логика OTP:
    - Получение текущей конфигурации (длина кода, время жизни).
    - Генерация кода с использованием `SecureRandom` согласно заданной длине.
    - Сохранение кода в базу с привязкой к пользователю и операции.
    - Проверка, что для операции ещё нет активного кода.
    - Валидация: сверяет код, проверяет срок действия, переводит в статус `USED` или `EXPIRED`.
    - Обновление конфигурации администратором.

### Работа с каналами доставки

- **`NotificationDispatcher`** — диспетчер, который принимает набор каналов и вызывает соответствующие сервисы рассылки.
- **`NotificationService`** (интерфейс) — контракт для всех сервисов рассылки с единственным методом `sendCode(recipient, code)`.

#### Реализации `NotificationService`:

- **`EmailNotificationService`** — отправка кода по электронной почте.
- **`SmsNotificationService`** — отправка SMS через SMPP-эмулятор (например, SMPPsim). Использует библиотеку jSMPP для связывания с сервером и отправки короткого сообщения.
- **`TelegramNotificationService`** — отправка кода через Telegram Bot API.
- **`FileNotificationService`** — сохраняет код в файл `otp_codes.log` в корне проекта (дописывает строку с меткой времени и именем пользователя).

## 9. Планировщик задач

- **`OtpExpirationScheduler`** — компонент с аннотацией `@Scheduled`. Раз в 30 секунд вызывает репозиторный метод, который массово обновляет все активные OTP, у которых истёк срок, на статус `EXPIRED`.

## 10. Контроллеры (представительский слой)

- **`AuthController`** — открытые эндпоинты `/api/auth/register` и `/api/auth/login`. Принимают DTO, логируют вызовы и возвращают результат.
- **`AdminController`** — эндпоинты для администратора, доступны только при наличии роли `ADMIN` (проверка через `@PreAuthorize`):
    - `PUT /api/admin/otp-config` — изменение настроек OTP.
    - `GET /api/admin/users` — список пользователей (без админов).
    - `DELETE /api/admin/users/{id}` — удаление пользователя и его кодов.
- **`UserController`** — эндпоинты для обычного пользователя (роль `USER`):
    - `POST /api/user/otp/generate` — генерация и рассылка OTP. Из контекста безопасности извлекается ID текущего пользователя.
    - `POST /api/user/otp/validate` — проверка OTP по идентификатору операции и коду.

## 11. Вспомогательные файлы настройки (resources)

- **`application.properties`** — параметры подключения к PostgreSQL, настройки JPA, секрет и время жизни JWT, порт сервера, уровень логирования.
- **`email.properties`** — учётные данные SMTP-сервера.
- **`sms.properties`** — параметры подключения к SMPP-эмулятору.
- **`telegram.properties`** — токен бота и идентификатор чата по умолчанию.
- **`schema.sql`** (опциональный) — скрипт ручного создания таблиц, если не используется автоматическое создание через Hibernate.