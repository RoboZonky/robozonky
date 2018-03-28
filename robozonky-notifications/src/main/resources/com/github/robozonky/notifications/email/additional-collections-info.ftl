<#assign items=data.collections>
<#if items?size == 0>
Oddělení vymáhání Zonky nedodalo žádné informace.
<#else>
Poslední informace z oddělení vymáhání Zonky:

<#list items as x>
- [${x.code}, <#if x.endDate??>${x.startDate?date} až ${x.endDate?date}<#else>${x.startDate?date}</#if>] ${x.note}
</#list>
</#if>
