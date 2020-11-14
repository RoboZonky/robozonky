<#setting locale="cs_CZ">
<#macro date d>${d?string["dd. MM. yyyy"]}</#macro>
<#macro datetime d><@date d /> v ${d?string["HH:mm:ss '('zzz')'"]}</#macro>
<#macro idRating id>
    <#switch id>
        <#case "2.99">
            <#assign label = "2,99">
            <#break>
        <#case "3.99">
            <#assign label = "3,99">
            <#break>
        <#case "4.99">
            <#assign label = "4,99">
            <#break>
        <#case "5.99">
            <#assign label = "5,99">
            <#break>
        <#case "6.99">
            <#assign label = "6,99">
            <#break>
        <#case "8.49">
            <#assign label = "8,49">
            <#break>
        <#case "9.49">
            <#assign label = "9,49">
            <#break>
        <#case "10.99">
            <#assign label = "10,99">
            <#break>
        <#case "13.49">
            <#assign label = "13,49">
            <#break>
        <#case "15.49">
            <#assign label = "15,49">
            <#break>
        <#case "19.99">
            <#assign label = "19,99">
            <#break>
        <#default>
            <#assign label = "---">
    </#switch>
${label?left_pad(5)} % p.a.</#macro>
<#if data.session.isDryRun>
POZOR: RoboZonky běží ve zkušebním režimu. Následující informace slouží jen pro demonstraci nastavení a nemusí být
platné ani úplné!
===============================================================================
</#if>

<#include embed>

--
Tuto zprávu dostáváte, protože je tak Váš robot nakonfigurován. Neodpovídejte na ni.

Údaje v této zprávě jsou pouze orientační a mohou obsahovat chyby. Směrodatná data poskytuje výhradně Zonky dashboard.
Výnosy jsou uváděny po započtení investorských poplatků vč. případných slev a před zdaněním. Na celé portfolio je
aplikována aktuální platná výše poplatků, stará portfolia s nižšími poplatky tedy mohou vykazovat výkonnost vyšší než
uvedenou.

Dotazy k RoboZonky pokládejte v uživatelské skupině:
https://groups.google.com/forum/#!forum/robozonky-users

Vygeneroval ${data.session.userAgent} pro Zonky účet ${data.session.userName}
dne <@datetime timestamp /> na základě systémové události vytvořené dne
<@datetime data.conception />.
