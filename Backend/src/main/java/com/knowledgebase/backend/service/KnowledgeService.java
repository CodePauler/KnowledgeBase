package com.knowledgebase.backend.service;

import com.knowledgebase.backend.dao.KnowledgeMapper;
import com.knowledgebase.backend.dto.FileUploadResponseDto;
import com.knowledgebase.backend.dto.FileUploadResponseDto;
import com.knowledgebase.backend.dto.KnowledgeCreateRequestDto;
import com.knowledgebase.backend.dto.KnowledgeTreeNode;
import com.knowledgebase.backend.dto.KnowledgeUpdateRequestDto;
import com.knowledgebase.backend.entity.Knowledge;
import com.knowledgebase.backend.service.FileStorageInterface;
import com.knowledgebase.backend.service.FileDownloadDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeMapper knowledgeMapper;
    
    /**
     * 当只有一个实现类时，
     * Spring 会自动把这个实现注入到 FileStorageInterface 的依赖中；
     * 如果以后有多种实现（比如换存储），再用 @Qualifier 指定就可以。
     * 这样接口层依赖不绑死具体存储实现，更方便替换和测试。
     */
    private final FileStorageInterface fileStorageService;


    @Transactional
    public Knowledge create(KnowledgeCreateRequestDto req) {
        if (req.getSpaceId() == null) throw new IllegalArgumentException("spaceId不能为空");
        if (req.getTitle() == null || req.getTitle().isBlank()) throw new IllegalArgumentException("title不能为空");
        if (req.getType() == null) throw new IllegalArgumentException("type不能为空");

        // parentId 校验：必须属于同一个 space（避免跨 space 串树）
        if (req.getParentId() != null) {
            boolean ok = knowledgeMapper.existsInSpace(req.getParentId(), req.getSpaceId());
            if (!ok) throw new IllegalArgumentException("parentId不属于该space");
        }

        Knowledge k = Knowledge.builder()
                        .spaceId(req.getSpaceId())
                        .title(req.getTitle())
                        .type(req.getType())
                        .content(req.getContent())
                        .parentId(req.getParentId())
                        .blobKey(req.getBlobKey())
                        .build();

        knowledgeMapper.insert(k);
        return knowledgeMapper.selectById(k.getId());
    }

    public Knowledge get(Long id) {
        Knowledge k = knowledgeMapper.selectById(id);
        if (k == null) throw new NoSuchElementException("知识不存在");
        return k;
    }

    @Transactional
    public Knowledge update(Long id, KnowledgeUpdateRequestDto req) {
        // 简单存在性检查
        Knowledge existing = get(id);

        // parentId 校验：同一 space 且不能把自己挂自己下面
        if (req.getParentId() != null) {
            if (Objects.equals(req.getParentId(), id)) {
                throw new IllegalArgumentException("parentId不能是自己");
            }
            boolean ok = knowledgeMapper.existsInSpace(req.getParentId(), existing.getSpaceId());
            if (!ok) throw new IllegalArgumentException("parentId不属于该space");
        }

        knowledgeMapper.updateById(id, req.getTitle(), req.getContent(), req.getParentId(), req.getBlobKey());
        return knowledgeMapper.selectById(id);
    }

    // 因为表里 parent_id 外键是 ON DELETE CASCADE，所以子节点会一起删，需要开启Transactional
    @Transactional
    public void delete(Long id) {
        // 存在性检查
        get(id);
        knowledgeMapper.deleteById(id);
    }

    public List<Knowledge> listBySpace(Long spaceId) {
        return knowledgeMapper.listBySpace(spaceId);
    }

    public List<Knowledge> listChildren(Long spaceId, Long parentId) {
        return knowledgeMapper.listBySpaceAndParent(spaceId, parentId);
    }

    public List<KnowledgeTreeNode> tree(Long spaceId) {
        List<Knowledge> all = knowledgeMapper.listBySpace(spaceId);

        Map<Long, KnowledgeTreeNode> map = new HashMap<>(all.size() * 2);
        for (Knowledge k : all) {
            KnowledgeTreeNode n = KnowledgeTreeNode.builder()
                            .id(k.getId())
                            .spaceId(k.getSpaceId())
                            .title(k.getTitle())
                            .type(k.getType() == null ? null : k.getType().name())
                            .parentId(k.getParentId())
                            .build();
            map.put(n.getId(), n);
        }

        List<KnowledgeTreeNode> roots = new ArrayList<>();
        for (KnowledgeTreeNode n : map.values()) {
            if (n.getParentId() == null) {
                roots.add(n);
            } else {
                KnowledgeTreeNode p = map.get(n.getParentId());
                // 如果父节点缺失（脏数据），就当根
                if (p == null) roots.add(n);
                else p.getChildren().add(n);
            }
        }

        // 让目录稳定一点：按 id 排个序（也可按 title）
        sortTree(roots);
        return roots;
    }

    /**
     * @param id: 知识id
     * @param file: 上传的文件
     * @param category: 分类文件类别
     * @param userId: 用户id
     * @return FileUploadResponseDto 存储的是blobname
     * @description 上传知识关联的文件到OSS，并更新知识的blobKey字段
     */
    @Transactional
    public FileUploadResponseDto uploadOssFile(Long id, MultipartFile file, String category, Long userId) {
        Knowledge existing = get(id);
        FileUploadResponseDto res = fileStorageService.upload(file, category, userId);
        knowledgeMapper.updateBlobKey(id, res.getBlobKey());
        return res;
    }

    /**
     * @param id: 知识id
     * @return FileDownloadDto 文件下载DTO
     * @description 获取知识关联的文件下载流和头信息
     */
    public FileDownloadDto getKnowledgeFile(Long id) {
        Knowledge existing = get(id);
        if (existing.getBlobKey() == null || existing.getBlobKey().isBlank()) {
            throw new IllegalArgumentException("Knowledge has no file");
        }
        return fileStorageService.download(existing.getBlobKey());
    }

    private void sortTree(List<KnowledgeTreeNode> nodes) {
        nodes.sort(Comparator.comparing(KnowledgeTreeNode::getId));
        for (KnowledgeTreeNode n : nodes) {
            sortTree(n.getChildren());
        }
    }
}
