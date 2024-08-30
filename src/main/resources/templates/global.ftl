mode:
  type: Cluster
  repository:
    type: ZooKeeper
    props:
      namespace: ZA-${ip}:${port}
      server-lists: ${zookeeperIp}:2181
      retryIntervalMilliseconds: 500
      timeToLiveSeconds: 60
      maxRetries: 3
      operationTimeoutMilliseconds: 500
authority:
  users:
    - user: ${username}@%
      password: ${password}
  privilege:
    type: ALL_PERMITTED
logging:
  loggers:
    - loggerName: ShardingSphere-SQL
      additivity: true
      level: INFO
      props:
        enable: true
props:
  sql-show: true
  check-table-metadata-enabled: true
