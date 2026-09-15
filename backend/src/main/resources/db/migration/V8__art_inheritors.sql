-- ============================================================================
-- V8 · 补充 art.inheritors（非遗代表性传承人）
--
-- 背景：art 表 165 项非遗中原本仅 21 项有传承人记录（13%）。
-- 本脚本为其余 144 项补充代表性传承人，其中 88 项有权威依据、56 项保持为空。
--
-- 数据来源（权威）：文化和旅游部公布的《国家级非物质文化遗产代表性项目
-- 代表性传承人》第 1–6 批名单（共 4001 条），经项目名匹配后写入。
-- 仅收录官方名单中确实存在的传承人姓名；官方未公布个人传承人的项目
-- （多为「XX族服饰」类，官方以群体/地区为单位）一律保持 [] 而非编造。
--
-- 幂等：仅当该行 inheritors 为空数组时才写入，重复执行不会覆盖人工后续维护的数据。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V8__art_inheritors.sql
-- ============================================================================

DO $$
DECLARE
    updated integer := 0;
BEGIN
    UPDATE art SET inheritors = '["张秀芳","范祚信","周淑英"]'::jsonb
        WHERE name = '中国剪纸' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["容亚美","刘香兰","符林早"]'::jsonb
        WHERE name = '黎族传统纺染织绣技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["金明春","金明焕"]'::jsonb
        WHERE name = '朝鲜族农乐舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["艾力·依布拉音","塔力普·库宛"]'::jsonb
        WHERE name = '维吾尔族麦西热甫' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["吴品仙","胡官美","贾福英"]'::jsonb
        WHERE name = '侗族大歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["杜秀兰","白金花"]'::jsonb
        WHERE name = '裕固族民歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["杨似玉","杨求诗"]'::jsonb
        WHERE name = '侗族木构建筑营造技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["马进明"]'::jsonb
        WHERE name = '撒拉族篱笆楼营造技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["王国跃"]'::jsonb
        WHERE name = '羌族碉楼营造技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["金艾斯古丽·努尔坦阿肯"]'::jsonb
        WHERE name = '哈萨克族服饰' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["王菁"]'::jsonb
        WHERE name = '布依族服饰' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["马建新"]'::jsonb
        WHERE name = '撒拉族服饰' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["陈玉秋","徐永良"]'::jsonb
        WHERE name = '旗袍' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["何金秀","潘继凤","李素芳"]'::jsonb
        WHERE name = '瑶族服饰' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["兰曲钗"]'::jsonb
        WHERE name = '畲族凤凰装' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["柯璀玲"]'::jsonb
        WHERE name = '裕固族服饰' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["其木德"]'::jsonb
        WHERE name = '鄂温克族服饰' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["马舍勒"]'::jsonb
        WHERE name = '东乡族擀毡技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["和训","和世先"]'::jsonb
        WHERE name = '东巴画' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["和志本"]'::jsonb
        WHERE name = '东巴纸制作技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["娜汉"]'::jsonb
        WHERE name = '佤族织锦' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["马维雄","马尕主麻"]'::jsonb
        WHERE name = '保安族腰刀锻制技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["叶娟","玉儿甩"]'::jsonb
        WHERE name = '傣族织锦' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["穆永禄","沈占伟"]'::jsonb
        WHERE name = '回族砖雕' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["李发秀"]'::jsonb
        WHERE name = '土族盘绣' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["韦真礼"]'::jsonb
        WHERE name = '壮族铜鼓习俗' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["阿西巫之莫"]'::jsonb
        WHERE name = '彝族刺绣' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["杨腊三"]'::jsonb
        WHERE name = '德昂族酸茶制作技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["布如力·斯开克"]'::jsonb
        WHERE name = '柯尔克孜族刺绣' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["谭素娟"]'::jsonb
        WHERE name = '毛南族花竹帽' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["欧海金","杨胜昭"]'::jsonb
        WHERE name = '水书习俗' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["韦帮粉"]'::jsonb
        WHERE name = '水族剪纸' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["宋水仙","韦桃花"]'::jsonb
        WHERE name = '水族马尾绣' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["汪秀霞","赵志国"]'::jsonb
        WHERE name = '满族剪纸' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["邓菊花"]'::jsonb
        WHERE name = '瑶族刺绣' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["张仕绅","段银开"]'::jsonb
        WHERE name = '白族扎染' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["汪国芳","李兴秀"]'::jsonb
        WHERE name = '羌族刺绣' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["杨光宾","吴水根","邰引岩"]'::jsonb
        WHERE name = '苗族银饰锻制技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["叶水云","刘代娥"]'::jsonb
        WHERE name = '西兰卡普' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["尤文凤"]'::jsonb
        WHERE name = '赫哲族鱼皮制作技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["葛长云","满古梅"]'::jsonb
        WHERE name = '鄂伦春族狍皮制作技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["吴旭升"]'::jsonb
        WHERE name = '鄂温克族桦树皮制作技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["伊春光"]'::jsonb
        WHERE name = '锡伯族弓箭制作技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["项老赛"]'::jsonb
        WHERE name = '阿昌族户撒刀锻制技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["黄运英","王文奇"]'::jsonb
        WHERE name = '黎族树皮布制作技艺' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["谢忠厚"]'::jsonb
        WHERE name = '仫佬族依饭节师公舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["陈改保"]'::jsonb
        WHERE name = '佤族木鼓舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["朗四","波罕丙"]'::jsonb
        WHERE name = '傣族象脚鼓舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["龙正福"]'::jsonb
        WHERE name = '哈尼族棕扇舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["何桂英"]'::jsonb
        WHERE name = '基诺族大鼓舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["库尔班·托合塔什","买热木汗·阿地力"]'::jsonb
        WHERE name = '塔吉克族鹰舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["席玉秀"]'::jsonb
        WHERE name = '安昭舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["俸继明"]'::jsonb
        WHERE name = '布朗族蜂桶鼓舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["岩翁"]'::jsonb
        WHERE name = '德昂族水鼓舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["波益四"]'::jsonb
        WHERE name = '怒族达比亚舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["黄家近"]'::jsonb
        WHERE name = '打柴舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["李增保","李石开"]'::jsonb
        WHERE name = '拉祜族芦笙舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["田景仁","彭承金","田维政"]'::jsonb
        WHERE name = '摆手舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["赵明华","盘振松","黄道胜"]'::jsonb
        WHERE name = '瑶族长鼓舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["岳麻通","赵保忠"]'::jsonb
        WHERE name = '目瑙纵歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["吴玉华"]'::jsonb
        WHERE name = '达斡尔族鲁日格勒' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["月香"]'::jsonb
        WHERE name = '锡伯族贝伦舞' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["罗周文"]'::jsonb
        WHERE name = '京族哈节' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["杨朝忠"]'::jsonb
        WHERE name = '仡佬族傩戏' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["吴胜章","张启高","吴尚德"]'::jsonb
        WHERE name = '侗戏' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["李学强"]'::jsonb
        WHERE name = '傈僳族刀杆节' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["布比玛丽·贾合甫拜","加玛勒汗·哈拉巴特","哈斯木汗·瓦特汗"]'::jsonb
        WHERE name = '哈萨克族阿依特斯' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["胡宗显"]'::jsonb
        WHERE name = '土族轮子秋' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["李扎戈","李扎倮"]'::jsonb
        WHERE name = '拉祜族史诗《牡帕密帕》' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["谭三岗"]'::jsonb
        WHERE name = '毛南族肥套' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["张亚辉","杨正华"]'::jsonb
        WHERE name = '白族大本曲' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["马金山"]'::jsonb
        WHERE name = '东乡族花儿' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["苏春发"]'::jsonb
        WHERE name = '京族独弦琴艺术' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["毛呈祥"]'::jsonb
        WHERE name = '仡佬族民歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["王利","李学华","阿称恒"]'::jsonb
        WHERE name = '傈僳族民歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["车格","陈习娘"]'::jsonb
        WHERE name = '哈尼族多声部民歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["罗仕碧","田隆信","杨文明"]'::jsonb
        WHERE name = '土家族打溜子' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["热依木巴依·木巴拉克夏"]'::jsonb
        WHERE name = '塔吉克族民歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["梁秀江","吴天玉"]'::jsonb
        WHERE name = '布依八音坐唱' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["岩瓦洛"]'::jsonb
        WHERE name = '布朗族弹唱' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["阿迪里别克·卡德尔"]'::jsonb
        WHERE name = '库姆孜艺术' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["金星三"]'::jsonb
        WHERE name = '朝鲜族伽倻琴艺术' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["肯玉珍"]'::jsonb
        WHERE name = '独龙族民歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["雷美凤","蓝陈启"]'::jsonb
        WHERE name = '畲族山歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["龚代仁","陈海元"]'::jsonb
        WHERE name = '羌笛' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["莫金忠","孟满荣"]'::jsonb
        WHERE name = '达斡尔族民歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["额尔登掛","关金芳"]'::jsonb
        WHERE name = '鄂伦春族民歌（赞达仁）' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;
    UPDATE art SET inheritors = '["佟李美"]'::jsonb
        WHERE name = '锡伯族民歌' AND (inheritors IS NULL OR inheritors::text = '[]');
    IF FOUND THEN updated := updated + 1; END IF;

    RAISE NOTICE 'V8 传承人补充完成：本次更新 % 行（脚本内有值项目 % 个，保持为空 % 个）', updated, 88, 56;
END $$;

-- 校验：
-- SELECT count(*) FILTER (WHERE inheritors::text NOT IN ('[]','null')) AS with_inheritor, count(*) FROM art;
-- SELECT name, inheritors FROM art WHERE name = '侗族大歌';
