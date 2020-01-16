package com.github.ontio.mapper;

import com.github.ontio.model.dao.StorageNodeInfo;
import com.github.ontio.model.dao.StorageNodeInfoDetail;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;


@Repository
public interface StorageNodeInfoMapper extends Mapper<StorageNodeInfo> {

    Long selectStorageNodeCount();

    List<StorageNodeInfoDetail> selectStorageNode();

}