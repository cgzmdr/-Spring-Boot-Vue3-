package com.czdr.work.service;

import com.czdr.work.model.resource.LanguageAtlasResource;

/**
 * 民族语言文化专栏（B-3）
 *
 * @author cz
 */
public interface LanguageAtlasService {

    /**
     * 构建民族语文专栏数据：语系分布、文字一览、语言统计。
     */
    LanguageAtlasResource atlas();
}
