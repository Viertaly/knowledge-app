-- schema.sql
-- Скрипт инициализации схемы базы данных для проекта knowledge_db (MySQL)
-- Содержит таблицы: users, notes, note_links

-- Таблица `users`:
-- Хранит аккаунты пользователей приложения.
-- Поля:
--  - id: первичный ключ, автоинкремент
--  - username: уникальное имя пользователя
--  - password_hash: хранимая хэшированная версия пароля (временно TEXT)
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password_hash` TEXT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица `notes`:
-- Хранит заметки пользователя в древовидной структуре (parent_id указывает на родителя).
-- Поля:
--  - id: первичный ключ, автоинкремент
--  - title: заголовок заметки
--  - content: содержимое заметки
--  - user_id: владелец заметки (ссылка на users.id)
--  - parent_id: ссылка на родительскую заметку (self-reference)
--  - created_at: метка создания
CREATE TABLE IF NOT EXISTS `notes` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NOT NULL,
  `content` TEXT,
  `user_id` INT NOT NULL,
  `parent_id` INT DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notes_user_id` (`user_id`),
  KEY `idx_notes_parent_id` (`parent_id`),
  CONSTRAINT `fk_notes_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notes_parent` FOREIGN KEY (`parent_id`) REFERENCES `notes`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица `note_links`:
-- Хранит связи (ссылки) между заметками (например, ссылка из одной заметки на другую).
-- Поля:
--  - id: первичный ключ, автоинкремент
--  - source_note_id: исходная заметка
--  - target_note_id: целевая заметка
-- Дополнительно: уникальный составной индекс предотвращает дублирование одинаковых ссылок.
CREATE TABLE IF NOT EXISTS `note_links` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `source_note_id` INT NOT NULL,
  `target_note_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_note_links_source` (`source_note_id`),
  KEY `idx_note_links_target` (`target_note_id`),
  CONSTRAINT `fk_note_links_source` FOREIGN KEY (`source_note_id`) REFERENCES `notes`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_links_target` FOREIGN KEY (`target_note_id`) REFERENCES `notes`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uq_note_links_pair` UNIQUE (`source_note_id`, `target_note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
