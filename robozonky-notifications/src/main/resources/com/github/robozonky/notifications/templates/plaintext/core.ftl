<#setting locale="cs_CZ">
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
