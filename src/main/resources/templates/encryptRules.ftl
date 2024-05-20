columns:
<#list columns as column>
    ${column.name}:
    <#if column.assistedQuery??>
        assistedQuery:
        encryptorName: ${column.assistedQuery.encryptorName}
        name: ${column.assistedQuery.name}
    </#if>
    <#if column.cipher??>
        cipher:
        encryptorName: ${column.cipher.encryptorName}
        name: ${column.cipher.name}
    </#if>
    <#if column.likeQuery??>
        likeQuery:
        encryptorName: ${column.likeQuery.encryptorName}
        name: ${column.likeQuery.name}
    </#if>
    name: ${column.name}
</#list>
name: ${tableName}
