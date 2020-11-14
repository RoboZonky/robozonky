<#setting locale="cs_CZ">
<#macro idLoan data>
  <strong><a href="${data.loanUrl}"><q>${data.loanName?cap_first}</q> (${data.loanId?c})</a></strong>
</#macro>
<#macro zonky><em>Zonky</em></#macro>
<#macro robozonky><em>RoboZonky</em></#macro>
<#macro date d>${d?string["dd. MM. yyyy"]}</#macro>
<#macro datetime d><@date d /> v ${d?string["HH:mm:ss '('zzz')'"]}</#macro>

<#macro idRating id>
    <#switch id>
        <#case "2.99">
            <#assign label = "2,99">
            <#assign color = "c0498b">
            <#break>
        <#case "3.99">
            <#assign label = "3,99">
            <#assign color = "8b59be">
            <#break>
        <#case "4.99">
            <#assign label = "4,99">
            <#assign color = "596abe">
            <#break>
        <#case "5.99">
            <#assign label = "5,99">
            <#assign color = "599ebe">
            <#break>
        <#case "6.99">
            <#assign label = "6,99">
            <#assign color = "5abfa0">
            <#break>
        <#case "8.49">
            <#assign label = "8,49">
            <#assign color = "67cd75">
            <#break>
        <#case "9.49">
            <#assign label = "9,49">
            <#assign color = "91c95a">
            <#break>
        <#case "10.99">
            <#assign label = "10,99">
            <#assign color = "cebe5a">
            <#break>
        <#case "13.49">
            <#assign label = "13,49">
            <#assign color = "d7954b">
            <#break>
        <#case "15.49">
            <#assign label = "15,49">
            <#assign color = "e75637">
            <#break>
        <#case "19.99">
            <#assign label = "19,99">
            <#assign color = "d12f2f">
            <#break>
        <#default>
            <#assign label = "---">
            <#assign color = "000000">
    </#switch>
    <strong style="color: #${color};">${label} % p.a.</strong>
</#macro>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${data.subject}</title>
</head>

<body>
    <header>
        <h1>${data.subject}</h1>
    </header>

    <main>
        <#include embed>
    </main>

    <footer>
      <hr>
      <ul>
        <li><small>Tuto zprávu dostáváte, protože je tak <@robozonky /> nakonfigurován. Neodpovídejte na ni.</small></li>
        <#if data.session.isDryRun>
            <li><small><@robozonky /> běží ve zkušebním režimu. Uvedené informace slouží jen pro demonstraci nastavení a
                nemusí být platné ani úplné!</small></li>
        <#else>
            <li><small>Údaje v této zprávě jsou pouze orientační a mohou obsahovat chyby. Směrodatná data poskytuje
                výhradně <@zonky /> dashboard.</small></li>
            <li><small>Výnosy jsou uváděny po započtení investorských poplatků vč. případných slev a před zdaněním.
                Na celé portfolio je aplikována aktuální platná výše poplatků, stará portfolia s nižšími poplatky tedy
                mohou vykazovat výkonnost vyšší než uvedenou.</small></li>
        </#if>
        <li><small>Dotazy pokládejte
            <a href="https://groups.google.com/forum/#!forum/robozonky-users">v uživatelské skupině</a></small></li>
      </ul>
      <p>
        <small>Vygeneroval <em>${data.session.userAgent}</em> pro <@zonky /> účet <em>${data.session.userName}</em>
        dne <@datetime timestamp /> na základě systémové události vytvořené dne
        <@datetime data.conception />.</small>
      </p>
    </footer>
</body>

</html>
