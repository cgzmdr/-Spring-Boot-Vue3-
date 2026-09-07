package com.czdr.work.service;

import com.czdr.work.model.resource.SearchResultResource;

import java.util.List;

/**
 * @author cz
 */
public interface SearchService {
    SearchResultResource search(String q, String type, int page, int size);

    List<String> hot();
}
