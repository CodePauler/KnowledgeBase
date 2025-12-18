package com.knowledgebase.backend.dao;

import com.knowledgebase.backend.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeMapper {

    int insert(Knowledge knowledge);

    int updateById(@Param("id") Long id,
                   @Param("title") String title,
                   @Param("content") String content,
                   @Param("parentId") Long parentId,
                   @Param("blobKey") String blobKey);

    int updateBlobKey(@Param("id") Long id, @Param("blobKey") String blobKey);

    int updateParseJob(@Param("id") Long id, @Param("parseJob") String parseJob);

    int deleteById(@Param("id") Long id);

    Knowledge selectById(@Param("id") Long id);

    List<Knowledge> listBySpace(@Param("spaceId") Long spaceId);

    List<Knowledge> listBySpaceAndParent(@Param("spaceId") Long spaceId,
                                         @Param("parentId") Long parentId);

    boolean existsInSpace(@Param("id") Long id, @Param("spaceId") Long spaceId);
}
