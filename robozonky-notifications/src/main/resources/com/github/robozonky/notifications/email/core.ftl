<#setting locale="cs_CZ">
<#if data.isDryRun?? && data.isDryRun>
POZOR: RoboZonky běží ve zkušebním režimu. Následující informace slouží jen
pro demonstraci nastavení strategie a nejsou platné!
===============================================================================
</#if>

RoboZonky pro Zonky účet ${data.session.userName} Vás tímto informuje o následující operaci:

<#include embed>

--
Tuto zprávu dostáváte, protože je tak Váš robot nakonfigurován. Neodpovídejte na ni.

Dotazy k RoboZonky pokládejte v uživatelské skupině:
https://groups.google.com/forum/#!forum/robozonky-users

Vygeneroval ${data.session.userAgent} dne ${timestamp?date} v ${timestamp?time}.
