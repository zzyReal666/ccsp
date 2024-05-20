mode:
  type: ${modeType}
  repository:
    type: ${repositoryType}
    props:
    namespace: ${namespace}
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

rules:
- !ENCRYPT
  tables:
<#list tables as table>
    ${table.name}:
    columns:
    <#list table.columns as column>
        ${column.name}:
        cipher:
        name: ${column.cipherName}
        encryptorName: ${column.cipherEncryptorName}
        assistedQuery:
        name: ${column.assistedQueryName}
        encryptorName: ${column.assistedQueryEncryptorName}
        likeQuery:
        name: ${column.likeQueryName}
        encryptorName: ${column.likeQueryEncryptorName}
    </#list>
</#list>
  encryptors:
<#list encryptors as encryptor>
    ${encryptor.name}:
    type: ${encryptor.type}
    props:
    <#list encryptor.props as prop>
        ${prop.key}: ${prop.value}
    </#list>
</#list>

props:
  sql-show: true