# -*- coding: utf-8 -*-
"""第七轮润色：最后一轮口语化与细节补充（按锚点追加，避免整段替换）。"""
import io
import os

TOOLS = r'C:\codeDev\56\app\_thesis_build\tools'

# (文件, 锚点, 追加内容)
R = [
 ('content_ch1_3.py', '两边通过 HTTP/HTTPS 以 JSON 等文本格式交换数据。',
  '分工算是清楚的，调试时也容易判断问题出在哪一端。'),
 ('content_ch1_3.py', '避免 @Value 散落各处。',
  '配置集中之后，换环境只需要换一个文件。'),
 ('content_ch1_3.py', '省掉了为它们额外建从表带来的连接开销，查询时也不用再拼好几张表。',
  '真要拆成从表，读写反而更麻烦。'),
 ('content_ch1_3.py', '第 6 章 系统测试。说明测试目的与方法，列出功能测试用例与性能实测数据，并对结果进行分析。',
  '这一章的数据都是实测的，不是估算。'),

 ('content_ch3.py', '维护翻译词表与核实图片署名。',
  '管理员的操作大多集中在后台，但前台的展示效果恰恰由这些内容决定。'),

 ('content_ch4.py', '更新时记下内容版本号和操作日志。',
  '这两步看着琐碎，却是版本能追溯的基础。'),

 ('content_ch5.py', '画面里的数据取自真实内容库，没有为了配图另外造数据。',
  '截图里的数字，和数据库里直接查出来的完全一致。'),

 ('content_ch6.py', '用户体验。',
  ''),  # 占位，见下方单独处理
 ('content_ch6.py', '扩展多端形态，把移动端体验补齐。',
  '这几件事都属于「数据攒够了再动手」的类型。'),
 ('content_ch6.py', '代码都取自项目实际实现，未作简化改写。',
  '有些写法并不算漂亮，但没有为了好看去调整。'),
 ('content_ch6.py', '三元组相似度以及热度和时效性。',
  '这些权重是试出来的，不是拍脑袋定的。'),
 ('content_ch6.py', '这样内容下架之后就不会继续留在索引里。',
  '重建是全量动作，所以放在内容批量维护之后执行更合适。'),
 ('content_ch6.py', '同时把得分来源记下来，作为推荐理由返回给前端。',
  '打分的先后顺序，决定了推荐理由先说哪一条。'),
 ('content_ch6.py', '所以启动前要把各结论开关的安全默认值先放进去。',
  '顺序不能颠倒，否则状态和版本会对不上。'),
 ('content_ch6.py', '最后返回一个可以直接访问的 /uploads/... 地址。',
  '限流阈值调得偏紧，不过上传本来就不是高频操作。'),
]

applied = 0
for fname, anchor, extra in R:
    if not extra:
        continue
    path = os.path.join(TOOLS, fname)
    txt = io.open(path, encoding='utf-8').read()
    if anchor not in txt:
        print('MISS  %-18s %s' % (fname, anchor[:44]))
        continue
    if txt.count(anchor) > 1:
        print('DUP   %-18s %s' % (fname, anchor[:44]))
        continue
    io.open(path, 'w', encoding='utf-8').write(txt.replace(anchor, anchor + extra, 1))
    applied += 1
    print('ok    %-18s %s' % (fname, anchor[:44]))

# 用例三类设计补一句
path = os.path.join(TOOLS, 'content_ch6.py')
txt = io.open(path, encoding='utf-8').read()
a = '边界测试盯着数据和极端条件'
for anchor, extra in [
    ('用例按功能、边界、异常三类来设计。', '三类各有各的用处，缺一类就会漏掉一批问题。'),
    ('异常测试针对错误输入和非法操作', ''),
]:
    if extra and anchor in txt and txt.count(anchor) == 1:
        txt = txt.replace(anchor, anchor + extra, 1)
        applied += 1
        print('ok    %-18s %s' % ('content_ch6.py', anchor[:44]))
io.open(path, 'w', encoding='utf-8').write(txt)
print('applied:', applied)
