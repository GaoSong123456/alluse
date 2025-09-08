package com.routdata.financial.common.enums;

/**
 * 业务操作类型
 *
 * @author 络道科技
 * 有新增的类型加在最后面,数据库保存了ordinal()值
 */
public enum BusinessType {

    /**
     * 其它
     */
    OTHER,

    /**
     * 新增
     */
    INSERT,

    /**
     * 修改
     */
    UPDATE,

    /**
     * 删除
     */
    DELETE,

    /**
     * 授权
     */
    GRANT,

    /**
     * 导出
     */
    EXPORT,

    /**
     * 导入
     */
    IMPORT,

    /**
     * 强退
     */
    FORCE,

    /**
     * 生成代码
     */
    GENCODE,

    /**
     * 清空数据
     */
    CLEAN,

    /**
     * 上传文件
     */
    UPLOAD,

    /**
     * 下载文件
     */
    DOWNLOAD,
    /**
     * 查询
     */
    QUERY
}
