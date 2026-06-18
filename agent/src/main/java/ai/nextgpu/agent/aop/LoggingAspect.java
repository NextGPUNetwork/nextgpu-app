package ai.nextgpu.agent.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("@annotation(ai.nextgpu.agent.aop.Loggable)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("Executing: {} with arguments: {}", methodName, args);

        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("Completed: {} in {} ms with result: {}", methodName, elapsedTime, result);
            return result;
        } catch (Exception e) {
            log.error("Exception in {} - {}", methodName, e.getMessage(), e);
            throw e;
        }
    }

    @Pointcut("execution(* ai.nextgpu.agent.service..*(..)) && !within(ai.nextgpu.agent.service.ModelDownloadService)")
    public void serviceLayer() {
    }

    @Before("serviceLayer() && !@annotation(ai.nextgpu.agent.aop.Loggable)")
    public void before(JoinPoint joinPoint) {
        log.info("Executing method {}; args {}", joinPoint.getSignature(), trimArgs(joinPoint.getArgs()));
    }

    @AfterThrowing(value = "serviceLayer()", throwing = "throwable")
    public void afterThrowing(JoinPoint joinPoint, Throwable throwable) {
        log.error("Exception in {} - {}", joinPoint.getSignature(), throwable.getMessage());
    }

    /**
     * Trim argument if its string representation exceeds 50 characters
     *
     * @param args
     * @return
     */
    private String trimArgs(Object[] args) {
        return Arrays.stream(args)
                .map(this::formatResult)
                .collect(Collectors.joining(", "));
    }

    private String formatResult(Object obj) {
        if (obj == null) return "null";
        // Handle primitive wrappers explicitly
        if (obj instanceof Number || obj instanceof Boolean || obj instanceof Character) {
            return String.valueOf(obj);
        }
        // Handle arrays
        if (obj.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(obj);
            return "Array[" + length + "]";
        }
        // Handle collections
        if (obj instanceof Iterable<?>) {
            return "Collection[" + ((Iterable<?>) obj).spliterator().getExactSizeIfKnown() + "]";
        }
        // Convert to String and apply length constraint
        String str = obj.toString();
        return str.length() > 100 ? str.substring(0, 100) + "... [contents too long]" : str;
    }

}
