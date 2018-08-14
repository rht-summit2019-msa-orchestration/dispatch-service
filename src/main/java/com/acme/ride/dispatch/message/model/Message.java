package com.acme.ride.dispatch.message.model;

import java.util.Date;
import java.util.UUID;

public class Message<T> {

    private String messageType;
    private String id;
    private String traceId;
    private String sender;
    private Date timestamp;

    private T payload;

    public String getMessageType() {
        return messageType;
    }

    public String getId() {
        return id;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSender() {
        return sender;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public T getPayload() {
        return payload;
    }

    public static class Builder<T> {

        private final String messageType;
        private final String sender;
        private final T payload;

        private String id = UUID.randomUUID().toString();
        private String traceId;
        private Date timestamp = new Date();

        public Builder(String messageType, String sender, T payload) {

            this.messageType = messageType;
            this.sender = sender;
            this.payload = payload;
        }

        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        public Builder<T> traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder<T> timestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Message<T> build() {
            Message<T> msg = new Message<T>();
            msg.messageType = this.messageType;
            msg.sender = this.sender;
            msg.payload = this.payload;
            msg.id = this.id;
            msg.traceId = this.traceId;
            msg.timestamp = this.timestamp;
            return msg;
        }
    }

}
