mode:
  type: ${modeType}
  repository:
    type: ${repositoryType}
    props:
      namespace: namespace_${namespace}
      server-lists: ${serverLists}
      operationTimeoutMilliseconds: ${operationTimeoutMilliseconds}

dataSources:
  ds_0:
    dataSourceClassName: ${dataSourceClassName}
    driverClassName: ${driverClassName}
    ${urlKey}: ${url}
    username: ${username}
    password: ${password}
    maxPoolSize: ${maxPoolSize}
props:
  sql-show: true