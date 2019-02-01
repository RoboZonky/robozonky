<#setting locale="cs_CZ">
<#macro idLoan data>
  <strong><a href="${data.loanUrl}">č. ${data.loanId?c} <q>${data.loanName?cap_first}</q></a></strong>
</#macro>
<#macro zonky><em>Zonky</em></#macro>
<#macro robozonky><em>RoboZonky</em></#macro>
<#macro idRating id>
    <#switch id>
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
        <#case "8.49">
            <#assign label = "8,49">
            <#assign color = "67cd75">
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
        <p><small><@robozonky /> pro <@zonky /> účet <em>${data.session.userName}</em> Vás tímto informuje o
            následující skutečnosti:</small></p>
        <hr>
    </header>

    <main>
        <h1>${data.subject}</h1>
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
            <li><small>Údaje v této zprávě jsou pouze informativního charakteru a mohou obsahovat chyby. Směrodatná data
                poskytuje výhradně <@zonky /> dashboard.</small></li>
        </#if>
        <li><small>Dotazy pokládejte
            <a href="https://groups.google.com/forum/#!forum/robozonky-users">v uživatelské skupině</a></small></li>
      </ul>
      <p>
        <small>Vygeneroval <em>${data.session.userAgent}</em> v čase ${timestamp?datetime?iso_local_ms} na základě
        systémové události vytvořené v čase ${data.conception?datetime?iso_local_ms}.</small>
      </p>
    </footer>
</body>

</html>
