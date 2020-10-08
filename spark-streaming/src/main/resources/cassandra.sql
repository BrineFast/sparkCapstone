CREATE KEYSPACE botdetector WITH replication = {'class':'SimpleStrategy','replication_factor':1};
CREATE TABLE botdetector.structuredStreaming (
    id text,
    ip text,
    time bigin,
    eventType text,
    PRIMARY KEY(id));