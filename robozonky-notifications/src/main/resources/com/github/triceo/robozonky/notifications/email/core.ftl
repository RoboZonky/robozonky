<#if data.isDryRun?? && data.isDryRun>
POZOR: RoboZonky běží ve zkušebním režimu. Následující informace slouží jen
       pro demonstraci nastavení strategie a nejsou platné!
===============================================================================
</#if>

RoboZonky Vás tímto informuje o následujícím:

<#include embed>

--
Tuto zprávu dostáváte, protože je tak Váš robot nakonfigurován.

Vygeneroval ${robozonky} dne ${timestamp?date} v ${timestamp?time}.
