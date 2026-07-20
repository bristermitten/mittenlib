package me.bristermitten.mittenlib.annotations.integration;

import me.bristermitten.mittenlib.config.Config;

@Config
public interface NestedDefaultConfig {

    AuditLog auditLog();

    @Config
    interface AuditLog {
        default int flushIntervalSeconds() {
            return 5;
        }

        default int batchSize() {
            return 100;
        }
    }
}
