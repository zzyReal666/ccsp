
databaseName: ${databaseName}

dataSources:
  ds_0:
    url: ${url}
    username: ${username}
    password: ${password}
    connectionTimeoutMilliseconds: 30000
    idleTimeoutMilliseconds: 60000
    maxLifetimeMilliseconds: 1800000
    maxPoolSize: 50
    minPoolSize: 1
rules:
- !SINGLE
  tables:
    - ${singleTable}
