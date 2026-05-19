# Industrial Labs — Индексатор текстовых файлов

Многомодульный Maven-проект. Четыре лабы строятся последовательно: каждая использует код предыдущей.

## Структура

| Модуль | Что делает |
|---|---|
| `lab1-parser` | Парсинг одного файла → `Map<слово, кол-во>` |
| `lab2-indexer` | Рекурсивный обход директории, многопоточный индексатор |
| `lab3-database` | Сохранение индекса в PostgreSQL |
| `lab4-webapp` | Веб-приложение: поиск по словам + загрузка файлов |

## Требования

- Java 17+
- Maven
- Docker (для PostgreSQL, нужен lab3 и lab4)

## Запуск PostgreSQL

```bash
docker compose up -d
```

БД поднимается на порту **5434** (не конфликтует со стандартным 5432).  
Параметры: `host=localhost`, `port=5434`, `db=industrial_labs`, `user=admin`, `password=password`.

Остановить:
```bash
docker compose down
```

## Сборка всего проекта

```bash
mvn install
```

---

## Lab 1 — Парсинг файла

Читает `.txt` файл, считает количество вхождений каждого слова, выводит топ-20.

```bash
mvn compile exec:java -pl lab1-parser -Dexec.args="test-data/sample.txt"
```

---

## Lab 2 — Многопоточный индексатор

Рекурсивно обходит директорию, обрабатывает все `.txt` файлы параллельно (кол-во потоков = кол-во ядер CPU). Выводит статистику по каждому файлу и глобальный топ-10.

```bash
mvn compile exec:java -pl lab2-indexer -Dexec.args="test-data"
```

---

## Lab 3 — Сохранение в базу данных

Индексирует директорию (как lab2) и загружает результаты в PostgreSQL.

```bash
# 1. Поднять БД
docker compose up -d

# 2. Запустить индексацию
mvn compile exec:java -pl lab3-database -Dexec.args="test-data"
```

Схема БД:
```
files       (id, filename, filepath)
words       (id, word)
occurrences (file_id, word_id, count)
```

Проверить данные:
```bash
docker exec industrial_labs_db psql -U admin -d industrial_labs \
  -c "SELECT f.filename, w.word, o.count FROM occurrences o JOIN files f ON f.id=o.file_id JOIN words w ON w.id=o.word_id ORDER BY o.count DESC LIMIT 10;"
```

---

## Lab 4 — Веб-приложение

Сервлет-приложение на embedded Tomcat (порт 8080). Поиск по словам + загрузка файлов для индексации.

```bash
# 1. Поднять БД и загрузить начальные данные
docker compose up -d
mvn compile exec:java -pl lab3-database -Dexec.args="test-data"

# 2. Запустить веб-сервер
mvn compile exec:java -pl lab4-webapp
```

Открыть в браузере: **http://localhost:8080**

**Поиск** — ввести одно или несколько слов через пробел. Для каждого слова возвращается файл, в котором оно встречается наибольшее количество раз.

**Загрузка файла** — загрузить `.txt` файл, он автоматически индексируется и добавляется в БД. После загрузки по нему сразу можно искать.

## Тестовые данные

```
test-data/
├── sample.txt - тестовый текст
├── News/
│   └── новости
└── literature/
    └── отрывок из войны и мира
```
