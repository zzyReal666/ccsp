props:
  key-index: ${keyIndex}
<#list props as prop>
    ${prop.key}: ${prop.value}
</#list>
type: ${type}
