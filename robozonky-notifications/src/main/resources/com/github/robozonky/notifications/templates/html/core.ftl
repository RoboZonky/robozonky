<#setting locale="cs_CZ">
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${data.subject}</title>
</head>

<body>
    <header>
        <p><small><em>RoboZonky</em> pro <em>Zonky</em> účet <em>${data.session.userName}</em> Vás tímto informuje o
            následující skutečnosti:</small></p>
        <hr>
    </header>

    <main>
        <#include embed>
    </main>

    <footer>
      <hr>
      <ul>
        <li><small>Tuto zprávu dostáváte, protože je tak Váš robot nakonfigurován. Neodpovídejte na ni.</small></li>
        <#if data.session.isDryRun>
           <li>
                <small><em>RoboZonky</em> běží ve zkušebním režimu. Uvedené informace slouží jen pro demonstraci
                nastavení a nemusí být platné ani úplné!</small>
            </li>
        <#else>
           <li><small>Údaje v této zprávě jsou pouze informativního charakteru a mohou obsahovat chyby. Směrodatná data
           poskytuje výhradně <em>Zonky</em> dashboard.</small></li>
        </#if>
      </ul>
      <p>
        <small>Vygeneroval <em>${data.session.userAgent}</em> dne ${timestamp?date} v ${timestamp?time}. Dotazy
        pokládejte <a href="https://groups.google.com/forum/#!forum/robozonky-users">v uživatelské skupině</a>.</small>
      </p>
    </footer>

</body>

</html>
