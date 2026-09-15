package com.czdr.work.comment.convert;

import com.czdr.work.model.entity.Art;
import com.czdr.work.model.resource.ArtQueryInfoResource;
import com.czdr.work.util.JsonUtil;

/**
 * @author cz
 */
public class ArtConvert {

    private ArtConvert() {
    }

    public static ArtQueryInfoResource toInfoModel(Art entity) {
        return new ArtQueryInfoResource(
                entity.getId().toString(),
                entity.getName(),
                entity.getNameEn(),
                entity.getCategory(),
                entity.getEthnicGroup() != null ? entity.getEthnicGroup().getName() : null,
                entity.getDescription(),
                entity.getDescriptionEn(),
                entity.getDescriptionEnSource(),
                entity.getOrigin(),
                entity.getIntangibleHeritage(),
                JsonUtil.toStringArray(entity.getInheritors()),
                entity.getCoverImage()
        );
    }
}
