package com.czdr.work.comment.convert;

import com.czdr.work.model.entity.Festival;
import com.czdr.work.model.resource.FestivalQueryInfoResource;
import com.czdr.work.util.JsonUtil;

/**
 * @author cz
 */
public class FestivalConvert {

    private FestivalConvert() {
    }

    public static FestivalQueryInfoResource toInfoModel(Festival entity) {
        return new FestivalQueryInfoResource(
                entity.getId().toString(),
                entity.getName(),
                entity.getNameEn(),
                entity.getType(),
                entity.getEthnicGroup() != null ? entity.getEthnicGroup().getName() : null,
                entity.getSolarDate() != null ? entity.getSolarDate().toString() : null,
                entity.getLunarDate(),
                entity.getOrigin(),
                entity.getDescription(),
                entity.getDescriptionEn(),
                entity.getDescriptionEnSource(),
                JsonUtil.toStringArray(entity.getCustoms()),
                JsonUtil.toStringArray(entity.getImages()),
                entity.getCoverImage()
        );
    }
}
