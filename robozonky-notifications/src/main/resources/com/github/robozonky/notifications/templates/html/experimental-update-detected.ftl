<p><@robozonky /> komunita právě vydala novou experimentální verzi
<a href="${data.newVersion.url}">${data.newVersion.name}</a>! Soubory ke stažení:</p>

<ul>
    <#list data.newVersion.assets as asset>
        <li><a href="${asset.url}">${asset.name}</a> (${asset.megabytes} MB)</li>
    </#list>
</ul>

<p>Experimentální verze poskytují pohled do kuchyně <@robozonky /> a jsou určeny pro testování zkušenými uživateli.
Přinášejí nové funkce, které se v budoucnu mohou a nemusejí dostat do rukou běžným uživatelům.</p>

<p>Experimentální verze mohou obsahovat závažné chyby a neměly by být používány pro skutečné investování!</p>
