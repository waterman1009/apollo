package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.entity.Cluster;
import com.ctrip.framework.apollo.biz.service.ClusterService;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cluster Controller
 */
@RestController
public class ClusterController {

    @Autowired
    private ClusterService clusterService;

    /**
     * 创建 Cluster
     *
     * @param appId App 编号
     * @param autoCreatePrivateNamespace 是否自动创建 Cluster 自己的 Namespace
     * @param dto ClusterDTO
     * @return 保存后的 ClusterDTO
     */
    @RequestMapping(path = "/apps/{appId}/clusters", method = RequestMethod.POST)
    public ClusterDTO create(@PathVariable("appId") String appId,
                             @RequestParam(value = "autoCreatePrivateNamespace", defaultValue = "true") boolean autoCreatePrivateNamespace,
                             @RequestBody ClusterDTO dto) {
        // 校验 ClusterDTO 的 `name` 格式正确。
        if (!InputValidator.isValidClusterNamespace(dto.getName())) {
            throw new BadRequestException(String.format("Cluster格式错误: %s", InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE));
        }
        // 将 ClusterDTO 转换成 Cluster 对象
        Cluster entity = BeanUtils.transfrom(Cluster.class, dto);
        // 判断 `name` 在 App 下是否已经存在对应的 Cluster 对象。若已经存在，抛出 BadRequestException 异常。
        Cluster managedEntity = clusterService.findOne(appId, entity.getName());
        if (managedEntity != null) {
            throw new BadRequestException("cluster already exist.");
        }
        // 保存 Cluster 对象，并创建其 Namespace
        if (autoCreatePrivateNamespace) {
            entity = clusterService.saveWithInstanceOfAppNamespaces(entity);
        // 保存 Cluster 对象，不创建其 Namespace
        } else {
            entity = clusterService.saveWithoutInstanceOfAppNamespaces(entity);
        }
        // 将保存的 Cluster 对象转换成 ClusterDTO
        dto = BeanUtils.transfrom(ClusterDTO.class, entity);
        return dto;
    }

    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName:.+}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("appId") String appId,
                       @PathVariable("clusterName") String clusterName, @RequestParam String operator) {
        Cluster entity = clusterService.findOne(appId, clusterName);
        if (entity == null) {
            throw new NotFoundException("cluster not found for clusterName " + clusterName);
        }
        clusterService.delete(entity.getId(), operator);
    }

    @RequestMapping(value = "/apps/{appId}/clusters", method = RequestMethod.GET)
    public List<ClusterDTO> find(@PathVariable("appId") String appId) {
        List<Cluster> clusters = clusterService.findParentClusters(appId);
        return BeanUtils.batchTransform(ClusterDTO.class, clusters);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName:.+}", method = RequestMethod.GET)
    public ClusterDTO get(@PathVariable("appId") String appId,
                          @PathVariable("clusterName") String clusterName) {
        Cluster cluster = clusterService.findOne(appId, clusterName);
        if (cluster == null) {
            throw new NotFoundException("cluster not found for name " + clusterName);
        }
        return BeanUtils.transfrom(ClusterDTO.class, cluster);
    }

    @RequestMapping(value = "/apps/{appId}/cluster/{clusterName}/unique", method = RequestMethod.GET)
    public boolean isAppIdUnique(@PathVariable("appId") String appId,
                                 @PathVariable("clusterName") String clusterName) {
        return clusterService.isClusterNameUnique(appId, clusterName);
    }
}
