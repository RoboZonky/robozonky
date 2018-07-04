<#setting locale="cs_CZ">
<#macro idLoan data>
  <strong><a href="${data.loanUrl}">č. ${data.loanId?c} <q>${data.loanName?cap_first}</q></a></strong>
</#macro>
<#macro zonky><em>Zonky</em></#macro>
<#macro robozonky><em>RoboZonky</em></#macro>
<#macro idRating id>
    <#switch id>
        <#case "A**">
            <#assign color = "596abe">
            <#break>
        <#case "A*">
            <#assign color = "599ebe">
            <#break>
        <#case "A++">
            <#assign color = "59bea8">
            <#break>
        <#case "A+">
            <#assign color = "67cd75">
            <#break>
        <#case "A">
            <#assign color = "9acd67">
            <#break>
        <#case "B">
            <#assign color = "cebe5a">
            <#break>
        <#case "C">
            <#assign color = "d7954b">
            <#break>
        <#case "D">
            <#assign color = "e75637">
            <#break>
        <#default>
            <#assign color = "000000">
    </#switch>
    <strong style="color: #${color};">${id}</strong>
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
        <#include embed>
    </main>

    <footer>
      <hr>
      <ul>
        <li><small>Tuto zprávu dostáváte, protože je tak <@robozonky /> nakonfigurován. Neodpovídejte na ni.</small></li>
        <#if data.session.isDryRun>
           <li>
                <small><@robozonky /> běží ve zkušebním režimu. Uvedené informace slouží jen pro demonstraci
                nastavení a nemusí být platné ani úplné!</small>
            </li>
        <#else>
           <li><small>Údaje v této zprávě jsou pouze informativního charakteru a mohou obsahovat chyby. Směrodatná data
           poskytuje výhradně <@zonky /> dashboard.</small></li>
        </#if>
      </ul>
      <p>
        <small>Vygeneroval <em>${data.session.userAgent}</em> dne ${timestamp?date} v ${timestamp?time}. Dotazy
        pokládejte <a href="https://groups.google.com/forum/#!forum/robozonky-users">v uživatelské skupině</a>.</small>
      </p>
    </footer>
</body>

</html>
