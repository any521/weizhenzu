package com.weizhenzu.common.exception;

import com.weizhenzu.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
