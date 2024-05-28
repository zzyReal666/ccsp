columns:
<#list columns as column>
 ${column.name}:
  cipher:
   encryptorName: ${column.cipher.encryptorName}
   name: ${column.cipher.name}
  name: ${column.name}
</#list>
name: ${tableName}


<#--   模糊查询和辅助查询
 <#if column.assistedQuery??>
        assistedQuery:
        encryptorName: ${column.assistedQuery.encryptorName}
        name: ${column.assistedQuery.name}
    </#if>
    <#if column.likeQuery??>
        likeQuery:
        encryptorName: ${column.likeQuery.encryptorName}
        name: ${column.likeQuery.name}
    </#if>


    -->