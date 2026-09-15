package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.AutonomousArea;
import com.czdr.work.model.entity.PersonProfile;
import com.czdr.work.model.entity.TraditionalSport;
import com.czdr.work.model.entity.proxy.AutonomousAreaProxy;
import com.czdr.work.model.entity.proxy.PersonProfileProxy;
import com.czdr.work.model.entity.proxy.TraditionalSportProxy;
import com.czdr.work.model.request.AutonomousAreaSaveRequest;
import com.czdr.work.model.request.TraditionalSportSaveRequest;
import com.czdr.work.service.BSeriesAdminService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 后台 B 系列内容维护实现
 *
 * <p><b>唯一性约束处理</b>：三张表各有一个业务唯一键
 * （人物档案 = 姓名 + 民族；自治地方 = 名称；传统体育 = 名称）。
 * 这里在写入前显式校验，并抛出 {@link ErrorCode#DUPLICATE}，
 * 而不是依赖数据库约束报 500 —— 让后台能给出可读的错误提示。</p>
 *
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class BSeriesAdminServiceImpl implements BSeriesAdminService {

    /** 与 DiscussionServiceImpl 一致：项目未提供 ObjectMapper Bean，直接构建静态实例 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final EasyEntityQuery entityQuery;

    /* ===================== 人物档案 ===================== */

    @Override
    public EasyPageResult<PersonProfile> listPersons(String keyword, String roleType, String domain, Pageable pageable) {
        EasyPageResult<PersonProfile> page = entityQuery.queryable(PersonProfile.class)
                .where(p -> {
                    if (keyword != null && !keyword.isBlank()) {
                        p.or(() -> {
                            p.personName().like(keyword);
                            p.ethnicGroupName().like(keyword);
                        });
                    }
                    if (roleType != null && !roleType.isBlank()) {
                        p.roleType().eq(roleType);
                    }
                    if (domain != null && !domain.isBlank()) {
                        p.domain().eq(domain);
                    }
                })
                .orderBy(p -> p.personName().asc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        return new DefaultPageResult<>(page.getTotal(), page.getData());
    }

    @Override
    public PersonProfile getPerson(String id) {
        PersonProfile p = entityQuery.queryable(PersonProfile.class)
                .where(x -> x.id().eq(parseUuid(id)))
                .firstOrNull();
        if (p == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return p;
    }

    @Override
    @Transactional
    public String createPerson(PersonProfile body) {
        requireText(body.getPersonName(), "姓名不能为空");
        assertPersonUnique(body.getPersonName(), body.getEthnicGroupName(), null);

        UUID id = UUID.randomUUID();
        body.setId(id);
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(body).executeRows();
        return id.toString();
    }

    @Override
    @Transactional
    public void updatePerson(String id, PersonProfile body) {
        UUID uuid = parseUuid(id);
        PersonProfile exist = getPerson(id);
        requireText(body.getPersonName(), "姓名不能为空");
        assertPersonUnique(body.getPersonName(), body.getEthnicGroupName(), uuid);

        exist.setPersonName(body.getPersonName());
        exist.setEthnicGroupName(body.getEthnicGroupName());
        exist.setRoleType(body.getRoleType() != null && !body.getRoleType().isBlank()
                ? body.getRoleType() : "inheritor");
        exist.setDomain(body.getDomain());
        exist.setBio(body.getBio());
        exist.setLifespan(body.getLifespan());
        exist.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(exist).executeRows();
    }

    @Override
    @Transactional
    public void deletePerson(String id) {
        getPerson(id);
        // EasyQuery 默认禁止物理删除（防误删），此处业务上确需物理删除，显式放行
        entityQuery.deletable(PersonProfile.class)
                .allowDeleteStatement(true)
                .where(x -> x.id().eq(parseUuid(id)))
                .executeRows();
    }

    private void assertPersonUnique(String name, String ethnic, UUID excludeId) {
        PersonProfile dup = entityQuery.queryable(PersonProfile.class)
                .where(p -> {
                    p.personName().eq(name);
                    if (ethnic == null || ethnic.isBlank()) {
                        p.ethnicGroupName().isNull();
                    } else {
                        p.ethnicGroupName().eq(ethnic);
                    }
                    if (excludeId != null) {
                        p.id().ne(excludeId);
                    }
                })
                .firstOrNull();
        if (dup != null) {
            throw new BusinessException(ErrorCode.DUPLICATE,
                    "该「姓名 + 民族」的人物档案已存在（同名不同族是允许的，请检查民族是否填写正确）");
        }
    }

    /* ===================== 民族自治地方 ===================== */

    @Override
    public EasyPageResult<AutonomousArea> listAreas(String keyword, String level, String province, Pageable pageable) {
        EasyPageResult<AutonomousArea> page = entityQuery.queryable(AutonomousArea.class)
                .where(a -> {
                    if (keyword != null && !keyword.isBlank()) {
                        a.name().like(keyword);
                    }
                    if (level != null && !level.isBlank()) {
                        a.level().eq(level);
                    }
                    if (province != null && !province.isBlank()) {
                        a.province().eq(province);
                    }
                })
                .orderBy(a -> a.name().asc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        return new DefaultPageResult<>(page.getTotal(), page.getData());
    }

    @Override
    public AutonomousArea getArea(String id) {
        AutonomousArea a = entityQuery.queryable(AutonomousArea.class)
                .where(x -> x.id().eq(parseUuid(id)))
                .firstOrNull();
        if (a == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return a;
    }

    @Override
    @Transactional
    public String createArea(AutonomousAreaSaveRequest body) {
        requireText(body.getName(), "名称不能为空");
        requireText(body.getLevel(), "级别不能为空");
        assertAreaUnique(body.getName(), null);

        AutonomousArea entity = new AutonomousArea();
        entity.setId(UUID.randomUUID());
        applyArea(entity, body);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(entity).executeRows();
        return entity.getId().toString();
    }

    @Override
    @Transactional
    public void updateArea(String id, AutonomousAreaSaveRequest body) {
        AutonomousArea exist = getArea(id);
        requireText(body.getName(), "名称不能为空");
        assertAreaUnique(body.getName(), parseUuid(id));

        applyArea(exist, body);
        exist.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(exist).executeRows();
    }

    /** 把请求 DTO 写入实体；jsonb 数组字段序列化为 JSON 文本 */
    private void applyArea(AutonomousArea entity, AutonomousAreaSaveRequest body) {
        entity.setName(body.getName());
        entity.setLevel(body.getLevel());
        entity.setEthnicGroups(writeJsonArray(body.getEthnicGroups()));
        entity.setProvince(body.getProvince());
        entity.setEstablishedYear(body.getEstablishedYear());
        entity.setSeat(body.getSeat());
    }

    @Override
    @Transactional
    public void deleteArea(String id) {
        getArea(id);
        entityQuery.deletable(AutonomousArea.class)
                .allowDeleteStatement(true)
                .where(x -> x.id().eq(parseUuid(id)))
                .executeRows();
    }

    private void assertAreaUnique(String name, UUID excludeId) {
        AutonomousArea dup = entityQuery.queryable(AutonomousArea.class)
                .where(a -> {
                    a.name().eq(name);
                    if (excludeId != null) {
                        a.id().ne(excludeId);
                    }
                })
                .firstOrNull();
        if (dup != null) {
            throw new BusinessException(ErrorCode.DUPLICATE, "同名的民族自治地方已存在：" + name);
        }
    }

    /* ===================== 传统体育 ===================== */

    @Override
    public EasyPageResult<TraditionalSport> listSports(String keyword, String category, Pageable pageable) {
        EasyPageResult<TraditionalSport> page = entityQuery.queryable(TraditionalSport.class)
                .where(s -> {
                    if (keyword != null && !keyword.isBlank()) {
                        s.name().like(keyword);
                    }
                    if (category != null && !category.isBlank()) {
                        s.category().eq(category);
                    }
                })
                .orderBy(s -> s.name().asc())
                .toPageResult(pageable.getPageNumber(), pageable.getPageSize());
        return new DefaultPageResult<>(page.getTotal(), page.getData());
    }

    @Override
    public TraditionalSport getSport(String id) {
        TraditionalSport s = entityQuery.queryable(TraditionalSport.class)
                .where(x -> x.id().eq(parseUuid(id)))
                .firstOrNull();
        if (s == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return s;
    }

    @Override
    @Transactional
    public String createSport(TraditionalSportSaveRequest body) {
        requireText(body.getName(), "项目名称不能为空");
        requireText(body.getCategory(), "类别不能为空");
        assertSportUnique(body.getName(), null);

        TraditionalSport entity = new TraditionalSport();
        entity.setId(UUID.randomUUID());
        applySport(entity, body);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(entity).executeRows();
        return entity.getId().toString();
    }

    @Override
    @Transactional
    public void updateSport(String id, TraditionalSportSaveRequest body) {
        TraditionalSport exist = getSport(id);
        requireText(body.getName(), "项目名称不能为空");
        assertSportUnique(body.getName(), parseUuid(id));

        applySport(exist, body);
        exist.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(exist).executeRows();
    }

    /** 把请求 DTO 写入实体；jsonb 数组字段序列化为 JSON 文本 */
    private void applySport(TraditionalSport entity, TraditionalSportSaveRequest body) {
        entity.setName(body.getName());
        entity.setCategory(body.getCategory());
        entity.setEthnicOrigins(writeJsonArray(body.getEthnicOrigins()));
        entity.setDescription(body.getDescription());
        entity.setEquipment(body.getEquipment());
        entity.setVenue(body.getVenue());
        entity.setTeamSize(body.getTeamSize());
        entity.setFirstEventYear(body.getFirstEventYear());
        entity.setSubEvents(writeJsonArray(body.getSubEvents()));
        entity.setHeritageLink(body.getHeritageLink());
    }

    @Override
    @Transactional
    public void deleteSport(String id) {
        getSport(id);
        entityQuery.deletable(TraditionalSport.class)
                .allowDeleteStatement(true)
                .where(x -> x.id().eq(parseUuid(id)))
                .executeRows();
    }

    private void assertSportUnique(String name, UUID excludeId) {
        TraditionalSport dup = entityQuery.queryable(TraditionalSport.class)
                .where(s -> {
                    s.name().eq(name);
                    if (excludeId != null) {
                        s.id().ne(excludeId);
                    }
                })
                .firstOrNull();
        if (dup != null) {
            throw new BusinessException(ErrorCode.DUPLICATE, "同名的传统体育项目已存在：" + name);
        }
    }

    /* ===================== 公共 ===================== */

    /**
     * 把名称数组序列化为 jsonb 列所需的 JSON 文本。
     * <p>实体中的 jsonb 字段以 String 承载，必须由服务层完成序列化 ——
     * 若直接用实体接请求体，前端传来的 JSON 数组无法写入 String 字段。</p>
     */
    private String writeJsonArray(List<String> list) {
        try {
            List<String> clean = list == null
                    ? List.of()
                    : list.stream()
                            .filter(s -> s != null && !s.isBlank())
                            .map(String::trim)
                            .distinct()
                            .toList();
            return OBJECT_MAPPER.writeValueAsString(clean);
        } catch (Exception e) {
            return "[]";
        }
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID 格式不正确");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
}
