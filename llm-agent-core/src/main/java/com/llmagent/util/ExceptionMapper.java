package com.llmagent.util;


import com.llmagent.exception.*;

import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.Callable;

@FunctionalInterface
public interface ExceptionMapper {
    ExceptionMapper DEFAULT = new DefaultExceptionMapper();

    static <T> T mappingException(Callable<T> action) {
        return DEFAULT.withExceptionMapper(action);
    }

    default <T> T withExceptionMapper(Callable<T> action) {
        try {
            return action.call();
        } catch (Exception e) {
            throw mapException(e);
        }
    }

    RuntimeException mapException(Exception e);

    class DefaultExceptionMapper implements ExceptionMapper {

        @Override
        public RuntimeException mapException(Exception e) {
            Throwable rootCause = findRoot(e);

            if (rootCause instanceof HttpException httpException) {
                return mapHttpStatusCode(httpException, httpException.statusCode());
            }

            if (rootCause instanceof UnresolvedAddressException) {
                return new UnresolvedModelServerException(rootCause);
            }

            return e instanceof RuntimeException re ? re : new LlmAgentException(e);
        }

        protected RuntimeException mapHttpStatusCode(Exception rootException, int httpStatusCode) {
            if (httpStatusCode >= 500 && httpStatusCode < 600) {
                return new InternalServerException(rootException);
            }
            if (httpStatusCode == 401 || httpStatusCode == 403) {
                return new AuthenticationException(rootException);
            }
            if (httpStatusCode == 404) {
                return new ModelNotFoundException(rootException);
            }
            if (httpStatusCode == 408) {
                return new TimeoutException(rootException);
            }
            if (httpStatusCode == 429) {
                return new RateLimitException(rootException);
            }
            if (httpStatusCode >= 400 && httpStatusCode < 500) {
                return new InvalidRequestException(rootException);
            }
            return rootException instanceof RuntimeException re ? re : new LlmAgentException(rootException);
        }

        private static Throwable findRoot(Throwable e) {
            Throwable cause = e.getCause();
            return cause == null || cause == e ? e : findRoot(cause);
        }
    }
}
