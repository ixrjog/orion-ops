package com.orion.ops.entity.request;

import com.orion.ops.consts.file.FileDownloadType;
import lombok.Data;

/**
 * 文件下载请求
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2021/6/8 17:08
 */
@Data
public class FileDownloadRequest {

    /**
     * id
     */
    private Long id;

    /**
     * @see FileDownloadType
     */
    private Integer type;

    /**
     * 下载token
     */
    private String token;

}