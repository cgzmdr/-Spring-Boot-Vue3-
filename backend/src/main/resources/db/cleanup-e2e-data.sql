-- ============================================================================
-- 手工工具：清理自动化验证产生的测试数据（不参与自动执行）
-- 用法：psql -h localhost -U postgres -d 56_app -f cleanup-e2e-data.sql
--
-- 匹配规则：昵称以「测试用户 / 社交 / 订阅 / 提及 / 前端 / 社区 / 调试 / 角标 / 联动 / 举报调试 / 翻译测试 / 公告测试 / 词表测试 / 共测 / AITEST / PROBE」开头；
-- 另外删除名称为「测试板块…」的板块及其帖子、标题以「E2E 」开头的站内公告，
-- 以及本机验证产生的机器翻译缓存（译文可按原文重新生成，属可丢弃数据）。
-- 执行前请确认这些前缀不会误伤真实用户（生产环境请勿执行）。
-- ============================================================================

BEGIN;

CREATE TEMP TABLE tmp_users ON COMMIT DROP AS
SELECT id FROM user_account
WHERE nickname ~ '^(测试用户|社交|订阅|提及|前端|社区|调试|角标|联动|举报调试|订阅前端|消息|私信|优先级作者|翻译测试|公告测试|词表测试|共测|AITEST|PROBE)';

CREATE TEMP TABLE tmp_topics ON COMMIT DROP AS
SELECT t.id FROM discussion_topic t
WHERE t.author_id IN (SELECT id FROM tmp_users)
   OR t.board_id IN (SELECT id FROM discussion_board WHERE name LIKE '测试板块%' OR name LIKE '优先级板块%');

CREATE TEMP TABLE tmp_conversations ON COMMIT DROP AS
SELECT c.id FROM discussion_conversation c
WHERE c.user_a IN (SELECT id FROM tmp_users) OR c.user_b IN (SELECT id FROM tmp_users);

-- 社区内容与关系
DELETE FROM discussion_message WHERE conversation_id IN (SELECT id FROM tmp_conversations);
DELETE FROM discussion_conversation WHERE id IN (SELECT id FROM tmp_conversations);
DELETE FROM discussion_post
 WHERE author_id IN (SELECT id FROM tmp_users)
    OR topic_id IN (SELECT id FROM tmp_topics);
DELETE FROM discussion_topic WHERE id IN (SELECT id FROM tmp_topics);
DELETE FROM discussion_report
 WHERE reporter_id IN (SELECT id FROM tmp_users)
    OR target_id IN (SELECT id FROM tmp_topics);
DELETE FROM discussion_subscription
 WHERE user_id IN (SELECT id FROM tmp_users)
    OR (target_type = 'topic' AND target_id IN (SELECT id FROM tmp_topics))
    OR (target_type = 'board' AND target_id IN (SELECT id FROM discussion_board WHERE name LIKE '测试板块%'));
DELETE FROM discussion_follow
 WHERE user_id IN (SELECT id FROM tmp_users) OR follow_user_id IN (SELECT id FROM tmp_users);
DELETE FROM notification
 WHERE user_id IN (SELECT id FROM tmp_users)
    OR actor_id IN (SELECT id FROM tmp_users)
    OR (target_type = 'discussion_topic' AND target_id IN (SELECT id FROM tmp_topics))
    OR title LIKE 'E2E %';
DELETE FROM discussion_moderation_log
 WHERE operator_id IN (SELECT id FROM tmp_users)
    OR target_type = 'broadcast'
    OR reason LIKE 'E2E %';

-- 机器翻译缓存（纯缓存，可重新生成）：清理「原文已不存在」的孤儿译文
DELETE FROM discussion_translation t
 WHERE (t.target_type = 'topic' AND NOT EXISTS (SELECT 1 FROM discussion_topic x WHERE x.id = t.target_id))
    OR (t.target_type = 'post' AND NOT EXISTS (SELECT 1 FROM discussion_post x WHERE x.id = t.target_id))
    OR (t.target_type = 'message' AND NOT EXISTS (SELECT 1 FROM discussion_message x WHERE x.id = t.target_id))
    OR (t.target_type = 'board' AND NOT EXISTS (SELECT 1 FROM discussion_board x WHERE x.id = t.target_id));

-- 测试板块（及其帖子已在上方删除）
DELETE FROM discussion_board WHERE name LIKE '测试板块%' OR name LIKE '优先级板块%';

-- 自动化导入的敏感词（如「导入词甲xxxxxx」）与翻译词表测试词条（如「E2E术语xxxxxx」）
DELETE FROM sensitive_word WHERE word LIKE '导入词%';
DELETE FROM translate_glossary WHERE term LIKE 'E2E%';

-- 互动计数与用户本体
DELETE FROM favorite WHERE user_id IN (SELECT id FROM tmp_users);
DELETE FROM like_record WHERE user_id IN (SELECT id FROM tmp_users);
DELETE FROM user_role WHERE user_id IN (SELECT id FROM tmp_users);
DELETE FROM user_account WHERE id IN (SELECT id FROM tmp_users);

COMMIT;

-- 校验
-- SELECT count(*) AS users FROM user_account;
-- SELECT name, status, post_policy FROM discussion_board ORDER BY order_num;
