package com.llmagent.mcp.server.http;

import java.io.Serializable;

public sealed interface HttpStatusCode extends Serializable permits DefaultHttpStatusCode, HttpStatus {

    /**
     * Return the integer value of this status code.
     */
    int value();

    /**
     * Whether this status code is in the Informational class ({@code 1xx}).
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.1">RFC 2616</a>
     */
    boolean is1xxInformational();

    /**
     * Whether this status code is in the Successful class ({@code 2xx}).
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.2">RFC 2616</a>
     */
    boolean is2xxSuccessful();

    /**
     * Whether this status code is in the Redirection class ({@code 3xx}).
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.3">RFC 2616</a>
     */
    boolean is3xxRedirection();

    /**
     * Whether this status code is in the Client Error class ({@code 4xx}).
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.4">RFC 2616</a>
     */
    boolean is4xxClientError();

    /**
     * Whether this status code is in the Server Error class ({@code 5xx}).
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.5">RFC 2616</a>
     */
    boolean is5xxServerError();

    /**
     * Whether this status code is in the Client or Server Error class
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.4">RFC 2616</a>
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.3">RFC 2616</a>
     * ({@code 4xx} or {@code 5xx}).
     * @see #is4xxClientError()
     * @see #is5xxServerError()
     */
    boolean isError();

    /**
     * Whether this {@code HttpStatusCode} shares the same integer {@link #value() value} as the other status code.
     * <p>Useful for comparisons that take deprecated aliases into account or compare arbitrary implementations
     * of {@code HttpStatusCode} (for example, in place of {@link HttpStatus#equals(Object) HttpStatus enum equality}).
     * @param other the other {@code HttpStatusCode} to compare
     * @return true if the two {@code HttpStatusCode} objects share the same integer {@code value()}, false otherwise
     * @since 6.0.5
     */
    default boolean isSameCodeAs(HttpStatusCode other) {
        return value() == other.value();
    }

    /**
     * Return an {@code HttpStatusCode} object for the given integer value.
     * @param code the status code as integer
     * @return the corresponding {@code HttpStatusCode}
     * @throws IllegalArgumentException if {@code code} is not a three-digit
     * positive number
     */
    static HttpStatusCode valueOf(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status != null) {
            return status;
        }
        else {
            return new DefaultHttpStatusCode(code);
        }
    }
}
