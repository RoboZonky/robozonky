<p><@robozonky /> komunita právě vydala novou verzi
<a href="${data.newVersion.url}">${data.newVersion.name}</a>! Soubory ke stažení:</p>

<ul>
    <#list data.newVersion.assets as asset>
        <li><a href="${asset.url}">${asset.name}</a> (${asset.megabytes} MB)</li>
    </#list>
</ul>

<p>Nové verze obvykle přináší opravy chyb nebo nové funkce - proto doporučujeme aktualizaci nainstalovat hned, jak to
bude možné. Ve výjimečných případech může být urychlený přechod na novou verzi dokonce nezbytný, neboť mohlo dojít k
úpravám na serveru <@zonky />, se kterými si stávající verze <@robozonky /> neumí poradit.</p>
