# MikroTik WireGuard Easy

Web-интерфейс для управления WireGuard на MikroTik RouterOS.

Backend написан на Kotlin и работает на Java 21 / Spring Boot 3.

Frontend: [MTWireGuardEasy-frontend](https://github.com/unlimmitted/MTWireGuardEasy-frontend)

## Требования

- MikroTik RouterBoard или MikroTik CHR;
- RouterOS 7.15 или новее;
- включённый RouterOS API;
- Docker с включённым BuildKit.

## Сборка Docker-образа

Из корня backend-репозитория выполните:

```bash
docker build --pull -t mtwgeasy:latest .
```

Во время сборки Docker автоматически:

1. загружает frontend;
2. устанавливает зависимости через `npm ci`;
3. собирает статические файлы;
4. собирает executable Spring Boot JAR;
5. создаёт runtime-образ только с Java 21 JRE.

По умолчанию используется ветка `master` frontend-репозитория. Для воспроизводимой сборки рекомендуется передавать тег или commit SHA:

```bash
docker build --pull \
  --build-arg FRONTEND_REF=FRONTEND_TAG_OR_COMMIT \
  -t mtwgeasy:latest .
```

Если нужно использовать fork frontend:

```bash
docker build --pull \
  --build-arg FRONTEND_REPOSITORY=https://github.com/OWNER/REPOSITORY.git \
  --build-arg FRONTEND_REF=BRANCH_TAG_OR_COMMIT \
  -t mtwgeasy:latest .
```

Повторные сборки используют кэши npm и Gradle. Чтобы принудительно получить свежее состояние изменяемой ветки frontend, выполните сборку с `--no-cache` либо передайте точный новый commit SHA в `FRONTEND_REF`.

## Запуск

Создайте постоянный Docker volume для SQLite-базы и запустите контейнер:

```bash
docker volume create mtwgeasy-data

docker run --name mtwgeasy \
  --detach \
  --restart unless-stopped \
  --publish 8080:8080 \
  --env GATEWAY=192.168.88.1 \
  --env MIKROTIK_USER=admin \
  --env MIKROTIK_PASSWORD=CHANGE_ME \
  --env IP_ROUTE_NAME=WGMTEasy \
  --volume mtwgeasy-data:/data \
  mtwgeasy:latest
```

После запуска откройте [http://localhost:8080](http://localhost:8080). Для входа используются значения `MIKROTIK_USER` и `MIKROTIK_PASSWORD`.

### Переменные окружения

| Переменная | Обязательная | Назначение | Значение по умолчанию |
| --- | --- | --- | --- |
| `GATEWAY` | да | IP-адрес или hostname MikroTik | — |
| `MIKROTIK_USER` | да | Пользователь RouterOS API и web-интерфейса | — |
| `MIKROTIK_PASSWORD` | да | Пароль RouterOS API и web-интерфейса | — |
| `IP_ROUTE_NAME` | нет | Комментарий маршрута, которым управляет приложение | `WGMTEasy` |
| `DB_PATH` | нет | Путь к SQLite-базе внутри контейнера | `/data/db.sqlite` |

Не передавайте пароль прямо в историю shell на общих системах. Для production используйте механизм secrets вашего оркестратора.

## Обновление

Соберите новый образ и пересоздайте контейнер, оставив тот же volume `mtwgeasy-data`. Данные статистики и настройки в SQLite сохранятся:

```bash
docker stop mtwgeasy
docker rm mtwgeasy

docker run --name mtwgeasy \
  --detach \
  --restart unless-stopped \
  --publish 8080:8080 \
  --env GATEWAY=192.168.88.1 \
  --env MIKROTIK_USER=admin \
  --env MIKROTIK_PASSWORD=CHANGE_ME \
  --env IP_ROUTE_NAME=WGMTEasy \
  --volume mtwgeasy-data:/data \
  mtwgeasy:latest
```

## Интерфейс

### Настройка WireGuard-сервера

![WireGuard server settings](https://github.com/user-attachments/assets/956d8d75-caaf-4135-ac1c-fe40fcccb047)

### Режим VPN-цепочки

Чтобы показать дополнительные настройки, включите **Double WireGuard VPN**. Конфигурацию также можно импортировать из файла WireGuard `.conf`.

![VPN chain mode](https://github.com/user-attachments/assets/db1b7cfb-501a-45e3-b398-2252fd386df1)

### Главный экран

![Main screen](https://github.com/user-attachments/assets/0ef41b8a-57da-4c79-8c8c-ae82245f43ed)

### Окно пира

![Peer modal](https://github.com/user-attachments/assets/578e0438-1879-4757-8443-76f33079d9eb)
