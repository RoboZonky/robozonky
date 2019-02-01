<#setting locale="cs_CZ">
<#macro idRating id>
    <#switch id>
        <#case "3.99">
            <#assign label = "3,99">
            <#break>
        <#case "4.99">
            <#assign label = "4,99">
            <#break>
        <#case "5.99">
            <#assign label = "5,99">
            <#break>
        <#case "8.49">
            <#assign label = "8,49">
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
POZOR: RoboZonky běží ve zkušebním režimu. Následující informace slouží jen
pro demonstraci nastavení a nemusí být platné ani úplné!
===============================================================================
</#if>

RoboZonky pro Zonky účet ${data.session.userName} Vás tímto informuje o následující skutečnosti:

<#include embed>

--
Tuto zprávu dostáváte, protože je tak Váš robot nakonfigurován. Neodpovídejte na ni.

Dotazy k RoboZonky pokládejte v uživatelské skupině:
https://groups.google.com/forum/#!forum/robozonky-users

Vygeneroval ${data.session.userAgent} v čase ${timestamp?datetime?iso_local_ms} na základě systémové události vytvořené v
čase ${data.conception?datetime?iso_local_ms}.
