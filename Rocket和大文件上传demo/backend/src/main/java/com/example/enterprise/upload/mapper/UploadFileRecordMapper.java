package com.example.enterprise.upload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.enterprise.upload.entity.UploadFileRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分片上传文件元数据 Mapper
 */
@Mapper
public interface UploadFileRecordMapper extends BaseMapper<UploadFileRecord> {
}
