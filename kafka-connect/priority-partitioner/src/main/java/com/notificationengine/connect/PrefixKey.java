package com.notificationengine.connect;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.transforms.Transformation;

import java.util.Map;

public class PrefixKey<R extends ConnectRecord<R>> implements Transformation<R> {

    @Override
    public R apply(R record) {
        if (record.key() == null) return record;
        String newKey = "priority-" + record.key().toString();
        return record.newRecord(record.topic(), record.kafkaPartition(), null, newKey,
                record.valueSchema(), record.value(), record.timestamp(), record.headers());
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef();
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}