package com.hyetaekon.hyetaekon.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class GlobalLoggingAspect {

    @Pointcut("execution(* com.hyetaekon.hyetaekon..*(..))")
    private void globalPointcut() {

    }

    @Before("globalPointcut()")
    public void logBeforeMethod(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.debug("[실행 메서드]: {} [매개변수]: {}", methodName, Arrays.toString(args));
    }

    @AfterReturning(value = "globalPointcut()", returning = "result")
    public void logAfterMethod(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().toShortString();

        log.debug("[종료 메서드]: {} [반환값]: {}", methodName, result);
    }

    @AfterThrowing(value = "globalPointcut()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {
        String methodName = joinPoint.getSignature().toShortString();

        log.error("[예외 발생 메서드]: {} [예외]: {}", methodName, ex.getMessage());
    }
}