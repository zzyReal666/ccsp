CASE
<#list props as prop>
    WHEN ${prop.name} = ${prop.plain} THEN ${cipher}
</#list>
ELSE age
END AS age