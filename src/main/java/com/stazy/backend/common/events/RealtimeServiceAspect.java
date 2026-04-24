package com.stazy.backend.common.events;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RealtimeServiceAspect {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeServiceAspect.class);
    private final RealtimeEventPublisher publisher;

    public RealtimeServiceAspect(RealtimeEventPublisher publisher) {
        this.publisher = publisher;
    }

    @AfterReturning(pointcut = "execution(public * com.stazy.backend..service.*Service.*(..)) " +
            "&& (execution(* create*(..)) || execution(* update*(..)) || execution(* delete*(..)) " +
            "|| execution(* accept*(..)) || execution(* reject*(..)) || execution(* review*(..)) " +
            "|| execution(* submit*(..)) || execution(* apply*(..)) || execution(* resolve*(..)) " +
            "|| execution(* close*(..)) || execution(* reopen*(..)) || execution(* finalize*(..)) " +
            "|| execution(* hire*(..)) || execution(* reply*(..)) || execution(* publish*(..)) " +
            "|| execution(* mark*(..)) || execution(* revoke*(..)) || execution(* goLive*(..)))",
            returning = "result")
    public void publishEvent(JoinPoint joinPoint, Object result) {
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        logger.debug("Intercepted modifying method: {}.{}", serviceName, methodName);
        
        // Extract entity name from service name (e.g. BookingService -> Booking)
        String entityName = serviceName.replace("Service", "").toLowerCase();
        
        String eventType = entityName + "_" + methodName;
        Object payload = result != null ? result : new ServiceActionEvent(serviceName, methodName);
        
        // Publish to global
        publisher.publishGlobalEvent(eventType, payload);
        
        // Publish to relevant roles based on entity
        if (serviceName.contains("Admin") || serviceName.contains("Listing") || serviceName.contains("Complaint")) {
            publisher.publishRoleEvent("ADMIN", eventType, payload);
            publisher.publishRoleEvent("SUPER_ADMIN", eventType, payload);
        }
        if (serviceName.contains("Booking") || serviceName.contains("Listing") || serviceName.contains("Owner")) {
            publisher.publishRoleEvent("OWNER", eventType, payload);
        }
        if (serviceName.contains("Student") || serviceName.contains("Booking") || serviceName.contains("Complaint")) {
            publisher.publishRoleEvent("STUDENT", eventType, payload);
        }
    }
}
