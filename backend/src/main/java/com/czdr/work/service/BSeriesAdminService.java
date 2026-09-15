package com.czdr.work.service;

import com.czdr.work.model.entity.AutonomousArea;
import com.czdr.work.model.entity.PersonProfile;
import com.czdr.work.model.entity.TraditionalSport;
import com.czdr.work.model.request.AutonomousAreaSaveRequest;
import com.czdr.work.model.request.TraditionalSportSaveRequest;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

/**
 * 后台管理：B 系列内容（人物档案 / 自治地方 / 传统体育）
 *
 * @author cz
 */
public interface BSeriesAdminService {

    /* 人物档案 */
    EasyPageResult<PersonProfile> listPersons(String keyword, String roleType, String domain, Pageable pageable);

    PersonProfile getPerson(String id);

    String createPerson(PersonProfile body);

    void updatePerson(String id, PersonProfile body);

    void deletePerson(String id);

    /* 民族自治地方（请求体用 DTO，jsonb 数组字段由服务层序列化） */
    EasyPageResult<AutonomousArea> listAreas(String keyword, String level, String province, Pageable pageable);

    AutonomousArea getArea(String id);

    String createArea(AutonomousAreaSaveRequest body);

    void updateArea(String id, AutonomousAreaSaveRequest body);

    void deleteArea(String id);

    /* 传统体育（请求体用 DTO，jsonb 数组字段由服务层序列化） */
    EasyPageResult<TraditionalSport> listSports(String keyword, String category, Pageable pageable);

    TraditionalSport getSport(String id);

    String createSport(TraditionalSportSaveRequest body);

    void updateSport(String id, TraditionalSportSaveRequest body);

    void deleteSport(String id);
}
