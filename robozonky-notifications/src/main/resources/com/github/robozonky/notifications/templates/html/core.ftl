<#setting locale="cs_CZ">
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${data.subject}</title>
</head>

<body>
    <header>
        <#if data.session.isDryRun>
           <p>
                <strong><em>RoboZonky</em> běží ve zkušebním režimu. Následující informace slouží jen pro demonstraci
                nastavení strategie a nejsou platné!</strong>
            </p>
        </#if>
        <p><em>RoboZonky</em> pro <em>Zonky</em> účet <em>${data.session.userName}</em> Vás tímto informuje o
            následující operaci:</p>
    </header>

    <main>
        <#include embed>
    </main>

    <footer>
      <p>Tuto zprávu dostáváte, protože je tak Váš robot nakonfigurován. Neodpovídejte na ni.</p>
      <p>Všechna data v této zprávě jsou pouze informativního charakteru. Jediné směrodatné údaje poskytuje
        <em>Zonky</em> dashboard.</p>
      <p>Dotazy k <em>RoboZonky</em> pokládejte v
        <a href="https://groups.google.com/forum/#!forum/robozonky-users">uživatelské skupině</a></p>
      <p>Vygeneroval <em>${data.session.userAgent}</em> dne ${timestamp?date} v ${timestamp?time}.</p>
    </footer>

</body>

</html>
