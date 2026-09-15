package com.czdr.work.service;

import com.czdr.work.model.entity.SensitiveWord;
import com.czdr.work.model.request.SensitiveWordRequest;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * 本地敏感词服务（纯本地匹配，不依赖第三方内容安全接口）
 *
 * @author cz
 */
public interface SensitiveWordService {

    /** 检测文本：返回命中等级、命中词与掩码后的文本 */
    SensitiveCheckResult check(String text);

    /** 词库变更后清空缓存，下次检测重新加载 */
    void refresh();

    /** 后台：分页查询词条 */
    EasyPageResult<SensitiveWord> list(String keyword, Pageable pageable);

    /** 后台：新增或更新词条（按 word 唯一） */
    String save(SensitiveWordRequest request, String currentWord);

    /** 后台：删除词条 */
    void delete(UUID id);

    /** 后台：批量导入（换行/逗号/顿号分隔），返回 { added, skipped } */
    java.util.Map<String, Object> importWords(String text, String level, String locale);

    /** 后台：导出全部启用词条（每行一条，可直接再导入） */
    java.util.List<String> exportWords();

    /** 后台：全部词条（供前端展示提示） */
    List<SensitiveWord> all();
}
