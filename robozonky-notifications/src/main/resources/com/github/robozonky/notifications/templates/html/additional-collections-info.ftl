<#assign items=data.collections>
<#if items?size == 0>
    <p>Oddělení vymáhání Zonky nedodalo žádné informace.</p>
<#else>
    <table>
        <caption>Poslední informace z oddělení vymáhání Zonky</caption>
        <thead>
        <tr>
            <th>Časová známka</th>
            <th>Událost</th>
            <th>Poznámka</th>
        </tr>
        </thead>
        <tbody>
            <#list items as x>
              <tr>
                <td><#if x.endDate??>${x.startDate?date} až ${x.endDate?date}<#else>${x.startDate?date}</#if></td>
                <td>${x.code}</td>
                <td>${x.note}</td>
              </tr>
            </#list>
        </tbody>
    </table>
</#if>
