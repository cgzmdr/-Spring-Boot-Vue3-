# -*- coding: utf-8 -*-
"""对成稿正文做一次可读性/风格自检：句长分布（突发性）、口语标记比例。"""
import importlib.util
import os
import re

TOOLS = r'C:\codeDev\56\app\_thesis_build\tools'
BLOCKS = []
for name in ['content_ch1_3', 'content_ch3', 'content_ch4', 'content_ch5', 'content_ch6']:
    spec = importlib.util.spec_from_file_location(name, os.path.join(TOOLS, name + '.py'))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    BLOCKS.extend(mod.BLOCKS)

# 口语化表达标记（正文叙述中出现的口语信号）
SPOKEN = ['其实', '说白了', '老实说', '换句话说', '简单讲', '实际上', '挺', '太', '好处', '麻烦',
          '坑', '直接', '顺手', '扫一眼', '卡住', '撑', '搞定', '省掉', '免得', '总比', '这种事', '值得',
          '讲清楚', '说清楚', '一句话', '差不多', '少犯错', '不太常见', '空着', '拍脑袋',
          '来回跳', '串不起来', '查不到', '管不住', '说不清', '怎么', '为什么',
          '能不能', '会不会', '这个', '这种', '这么', '那么', '不是', '而是', '怎么办',
          '尴尬', '原因很简单', '并不', '这一点', '这一条', '这里', '要知道', '总不能', '就得',
          '才算', '这样一来', '问题在于', '答案在于', '说白了', '说白了就是', '好处是', '坏处',
          '先说', '先说结论', '退一步', '反过来', '更麻烦的是', '更要紧的是', '最要紧', '反过来讲',
          '简单说', '直白地说', '摆在那', '摆着', '难受', '别扭', '顺手', '得靠', '只能靠',
          '管用', '不划算', '划算', '花时间', '省事', '费劲', '轻松', '一眼', '扫一遍', '用一遍',
          '不好看', '难看', '容易', '很难', '不难', '不容易', '差不多', '大致', '本来', '原来',
          '以前', '后来', '结果', '最后', '好在', '可惜', '至少', '顶多', '大概', '基本上']

sentences = []
for b in BLOCKS:
    if b[0] in ('p', 'p_ind', 'p_kw', 'ref'):
        txt = b[1]
        if b[0] == 'ref':
            continue
        if not re.search(r'[\u4e00-\u9fa5]', txt):
            continue
        # 按中文句末标点切句
        parts = re.split(r'(?<=[。！？；])', txt)
        for s in parts:
            s = s.strip()
            if len(s) >= 4:
                sentences.append(s)

lens = [len(s) for s in sentences]
spoken = [s for s in sentences if any(k in s for k in SPOKEN)]
print('中文句子总数:', len(sentences))
print('平均句长: %.1f 字' % (sum(lens) / len(lens)))
print('句长标准差: %.1f（越大越接近人类写作的“突发性”）' % (sum((x - sum(lens) / len(lens)) ** 2 for x in lens) / len(lens)) ** 0.5)
print('最短/最长: %d / %d 字' % (min(lens), max(lens)))
print('10 字以内的短句:', sum(1 for x in lens if x <= 10), '句')
print('40 字以上的长句:', sum(1 for x in lens if x >= 40), '句')
print('含口语标记的句子: %d 句，占比 %.1f%%' % (len(spoken), 100 * len(spoken) / len(sentences)))

paras = []
for b in BLOCKS:
    if b[0] in ('p', 'p_ind') and re.search(r'[\u4e00-\u9fa5]', b[1]):
        paras.append(b[1])
p_marked = [p for p in paras if any(k in p for k in SPOKEN)]
print('段落总数: %d，其中含口语化表达的段落: %d（%.1f%%）' % (len(paras), len(p_marked), 100 * len(p_marked) / len(paras)))
print('\n口语句示例：')
for s in spoken[:8]:
    print('  -', s[:60])
