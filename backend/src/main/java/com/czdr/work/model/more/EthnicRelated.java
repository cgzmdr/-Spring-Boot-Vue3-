package com.czdr.work.model.more;

import com.czdr.work.model.entity.AutonomousArea;
import com.czdr.work.model.entity.PersonProfile;

import java.util.List;

/**
 * 民族详情的关联信息。
 *
 * <p>把原先详情页没有展示、但库中确实存在的两个关联维度补齐：</p>
 * <ul>
 *   <li>{@code persons} —— 关联人物档案（代表性传承人 / 历史文化名家），
 *       按 {@code person_profile.ethnic_group_name} 与民族名匹配；</li>
 *   <li>{@code autonomousAreas} —— 以该民族为自治民族的自治地方，
 *       按 {@code autonomous_area.ethnic_groups}（JSON 数组）匹配；</li>
 *   <li>{@code populationRank} / {@code populationTotal} —— 人口排名与全国民族总数，
 *       用于「人口 第 N / 56」这类表述。</li>
 * </ul>
 *
 * @author cz
 */
public record EthnicRelated(
        List<PersonProfile> persons,
        List<AutonomousArea> autonomousAreas,
        int populationRank,
        int populationTotal
) {

    /** 空关联（无人物、无自治地方、无名次），供无关联数据的民族复用 */
    public static final EthnicRelated EMPTY = new EthnicRelated(List.of(), List.of(), 0, 0);
}
