package com.czdr.work.comment.convert;

import com.czdr.work.model.entity.Topic;
import com.czdr.work.model.entity.TopicEntry;
import com.czdr.work.model.resource.TopicEntryResource;
import com.czdr.work.model.resource.TopicQueryInfoResource;

import java.util.List;

/**
 * @author cz
 */
public class TopicConvert {

    private TopicConvert() {
    }

    public static TopicQueryInfoResource toInfoModel(Topic entity) {
        List<TopicEntryResource> entries = entity.getEntries() == null ? List.of()
                : entity.getEntries().stream().map(TopicConvert::toEntryModel).toList();
        return new TopicQueryInfoResource(
                entity.getId().toString(),
                entity.getSlug(),
                entity.getTitle(),
                entity.getSubtitle(),
                entity.getDescription(),
                entity.getCoverImage(),
                entity.getOrderNum(),
                entries
        );
    }

    private static TopicEntryResource toEntryModel(TopicEntry entry) {
        return new TopicEntryResource(
                entry.getId().toString(),
                entry.getEntryType(),
                entry.getEntryId().toString(),
                entry.getSortOrder()
        );
    }
}
