package com.memora.config;

import com.memora.common.exception.BusinessException;
import com.memora.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        log.warn("参数校验异常: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("参数校验失败");
        return Result.error(400, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("请求体参数校验异常: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("参数校验失败");
        return Result.error(400, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("路径参数类型异常: {}={}", e.getName(), e.getValue());
        return Result.error(400, "请求路径参数不合法");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        Throwable rootCause = e.getMostSpecificCause();
        String rootMessage = rootCause == null ? "" : rootCause.getMessage();
        log.warn("数据约束冲突: {}", rootMessage);
        return Result.error(409, resolveConflictMessage(rootMessage));
    }

    /**
     * 处理静态资源未找到异常（404）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("静态资源未找到: {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Result.error(404, "资源不存在"));
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e) {
        if (e instanceof NoResourceFoundException noResourceFoundException) {
            log.debug("静态资源未找到: {}", noResourceFoundException.getResourcePath());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(404, "资源不存在"));
        }
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.error("系统异常 requestId={}", requestId, e);
        return Result.error(500, "系统异常，请稍后重试（requestId: " + requestId + "）");
    }

    private String resolveConflictMessage(String rootMessage) {
        String normalized = rootMessage == null ? "" : rootMessage.toLowerCase();
        if (normalized.contains("uk_kb_tenant_slug")) {
            return "同一租户下已存在同名或同标识知识库";
        }
        if (normalized.contains("uk_doc_kb_path")) {
            return "当前目录下已存在同名文档或目录";
        }
        if (normalized.contains("uk_kb_member_user")) {
            return "知识库成员不能重复配置";
        }
        if (normalized.contains("uk_tenant_member")) {
            return "租户成员不能重复配置";
        }
        return "数据冲突，当前操作未能保存";
    }
}
