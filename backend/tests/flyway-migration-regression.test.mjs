import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const migrations = new URL('../src/main/resources/db/migration/', import.meta.url)

async function readMigration(name) {
  return readFile(new URL(name, migrations), 'utf8')
}

test('已执行的 AI 初始迁移保持首次发布内容', async () => {
  const [chat, knowledge] = await Promise.all([
    readMigration('V20260711_001__add_ai_chat_history_tables.sql'),
    readMigration('V20260711_002__add_ai_knowledge_tables.sql')
  ])
  const messageTable = chat.slice(chat.indexOf('CREATE TABLE `herb_ai_chat_message`'))

  assert.match(chat, /`deleted` TINYINT NOT NULL DEFAULT 0/)
  assert.match(chat, /UNIQUE KEY `uk_ai_chat_session_id` \(`session_id`\)/)
  assert.doesNotMatch(messageTable, /`user_id` BIGINT NULL/)
  assert.match(knowledge, /`deleted` TINYINT NOT NULL DEFAULT 0/g)
  assert.doesNotMatch(knowledge, /`is_deleted`/)
})

test('AI 表结构调整通过独立前向迁移完成', async () => {
  const forward = await readMigration('V20260713_002__forward_fix_ai_table_schema.sql')

  for (const token of [
    'herb_ai_chat_session',
    'herb_ai_chat_message',
    'herb_ai_knowledge_doc',
    'herb_ai_knowledge_chunk',
    'is_deleted',
    'user_id',
    'uk_ai_chat_session_user_id',
    'fk_ai_chat_message_session'
  ]) {
    assert.match(forward, new RegExp(token))
  }
  assert.match(forward, /information_schema\.columns/i)
  assert.match(forward, /information_schema\.statistics/i)
})

test('文件访问级别迁移兼容已存在字段和索引', async () => {
  const migration = await readMigration('V20260711_004__add_file_access_level.sql')

  assert.match(migration, /information_schema\.columns/i)
  assert.match(migration, /information_schema\.statistics/i)
  assert.match(migration, /PREPARE stmt/i)
})
