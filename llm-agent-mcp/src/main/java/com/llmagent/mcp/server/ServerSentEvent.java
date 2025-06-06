package com.llmagent.mcp.server;


import java.time.Duration;

import static com.llmagent.util.ObjectUtil.nullSafeEquals;
import static com.llmagent.util.ObjectUtil.nullSafeHash;
import static com.llmagent.util.StringUtil.replace;

/**
 * Representation for a Server-Sent Event for use with Spring's reactive Web support.
 * {@code Flux<ServerSentEvent>} or {@code Observable<ServerSentEvent>} is the
 * reactive equivalent to Spring MVC's {@code SseEmitter}.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> the type of data that this event contains
 * @see <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events</a>
 */
public class ServerSentEvent<T> {

    private final String id;

    private final String event;

    private final Duration retry;

    private final String comment;

    private final T data;


    private ServerSentEvent(String id, String event, Duration retry, String comment, T data) {
        this.id = id;
        this.event = event;
        this.retry = retry;
        this.comment = comment;
        this.data = data;
    }

    /**
     * Return the {@code id} field of this event, if available.
     */
    public String id() {
        return this.id;
    }

    /**
     * Return the {@code event} field of this event, if available.
     */
    public String event() {
        return this.event;
    }

    /**
     * Return the {@code retry} field of this event, if available.
     */
    public Duration retry() {
        return this.retry;
    }

    /**
     * Return the comment of this event, if available.
     */
    public String comment() {
        return this.comment;
    }

    /**
     * Return the {@code data} field of this event, if available.
     */
    public T data() {
        return this.data;
    }

    /**
     * Return a StringBuilder with the id, event, retry, and comment fields fully
     * serialized, and also appending "data:" if there is data.
     * @since 6.2.1
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        if (this.id != null) {
            appendAttribute("id", this.id, sb);
        }
        if (this.event != null) {
            appendAttribute("event", this.event, sb);
        }
        if (this.retry != null) {
            appendAttribute("retry", this.retry.toMillis(), sb);
        }
        if (this.comment != null) {
            sb.append(':').append(replace(this.comment, "\n", "\n:")).append('\n');
        }
        if (this.data != null) {
            sb.append("data:");
        }
        return sb.toString();
    }

    private void appendAttribute(String fieldName, Object fieldValue, StringBuilder sb) {
        sb.append(fieldName).append(':').append(fieldValue).append('\n');
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof ServerSentEvent<?> that &&
                nullSafeEquals(this.id, that.id) &&
                nullSafeEquals(this.event, that.event) &&
                nullSafeEquals(this.retry, that.retry) &&
                nullSafeEquals(this.comment, that.comment) &&
                nullSafeEquals(this.data, that.data)));
    }

    @Override
    public int hashCode() {
        return nullSafeHash(this.id, this.event, this.retry, this.comment, this.data);
    }

    @Override
    public String toString() {
        return ("ServerSentEvent [id = '" + this.id + "', event='" + this.event + "', retry=" +
                this.retry + ", comment='" + this.comment + "', data=" + this.data + ']');
    }


    /**
     * Return a builder for a {@code ServerSentEvent}.
     * @param <T> the type of data that this event contains
     * @return the builder
     */
    public static <T> Builder<T> builder() {
        return new BuilderImpl<>();
    }

    /**
     * Return a builder for a {@code ServerSentEvent}, populated with the given {@linkplain #data() data}.
     * @param <T> the type of data that this event contains
     * @return the builder
     */
    public static <T> Builder<T> builder(T data) {
        return new BuilderImpl<>(data);
    }

    /**
     * A mutable builder for a {@code ServerSentEvent}.
     *
     * @param <T> the type of data that this event contains
     */
    public interface Builder<T> {

        /**
         * Set the value of the {@code id} field.
         * @param id the value of the id field
         * @return {@code this} builder
         */
        Builder<T> id(String id);

        /**
         * Set the value of the {@code event} field.
         * @param event the value of the event field
         * @return {@code this} builder
         */
        Builder<T> event(String event);

        /**
         * Set the value of the {@code retry} field.
         * @param retry the value of the retry field
         * @return {@code this} builder
         */
        Builder<T> retry(Duration retry);

        /**
         * Set SSE comment. If a multi-line comment is provided, it will be turned into multiple
         * SSE comment lines as defined in Server-Sent Events W3C recommendation.
         * @param comment the comment to set
         * @return {@code this} builder
         */
        Builder<T> comment(String comment);

        /**
         * Set the value of the {@code data} field. If the {@code data} argument is a multi-line
         * {@code String}, it will be turned into multiple {@code data} field lines as defined
         * in the Server-Sent Events W3C recommendation. If {@code data} is not a String, it will
         * be encoded into JSON.
         * @param data the value of the data field
         * @return {@code this} builder
         */
        Builder<T> data(T data);

        /**
         * Builds the event.
         * @return the built event
         */
        ServerSentEvent<T> build();
    }


    private static class BuilderImpl<T> implements Builder<T> {

        private String id;

        private String event;

        private Duration retry;

        private String comment;

        private T data;

        public BuilderImpl() {
        }

        public BuilderImpl(T data) {
            this.data = data;
        }

        @Override
        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder<T> event(String event) {
            this.event = event;
            return this;
        }

        @Override
        public Builder<T> retry(Duration retry) {
            this.retry = retry;
            return this;
        }

        @Override
        public Builder<T> comment(String comment) {
            this.comment = comment;
            return this;
        }

        @Override
        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        @Override
        public ServerSentEvent<T> build() {
            return new ServerSentEvent<>(this.id, this.event, this.retry, this.comment, this.data);
        }
    }

}
