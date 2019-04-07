# RoboZonky

[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.github.robozonky/robozonky/badge.svg)](http://search.maven.org/#search|ga|1|robozonky-)
[![Linux Build Status](https://travis-ci.org/RoboZonky/robozonky.svg)](https://travis-ci.org/RoboZonky/robozonky)
[![Windows Build status](https://ci.appveyor.com/api/projects/status/o6983h25auupkt0p?svg=true)](https://ci.appveyor.com/project/triceo/robozonky-8acvb)
[![Code health](https://sonarcloud.io/api/project_badges/measure?project=com.github.robozonky%3Arobozonky&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.github.robozonky%3Arobozonky)

## Odkazy

* [Ke stažení](http://www.robozonky.cz) nebo [v Dockeru](https://hub.docker.com/r/robozonky/robozonky/).
* [Dokumentace](https://github.com/RoboZonky/robozonky/wiki).
* [Uživatelská podpora](https://groups.google.com/forum/#!forum/robozonky-users) pro dotazy a diskusi.
* [Hlášení chyb](https://github.com/RoboZonky/robozonky/issues) přímo na Githubu.

## Nahodilé poznámky pro vývojáře

* Všechny moduly kromě některých "koncových" (`installer`, `cli`) jsou modularizované podle JPMS. Změny, které nefungují
s JPMS, nebudou akceptovány. `installer` a `cli` modularizovány nejsou, protože jsou komzumovány jako uberjar a tedy
JPMS nedává smysl. 
* Pokud v nějakém modulu implementujete Javovské SPI (`ServiceLoader`), nezapomeňte vaší implementaci přidat nejen do 
`module-info.java` pod `provides` klauzuli (aby jí viděl `robozonky-app`), ale také postaru do `META-INF/services` 
(aby jí viděl nemodularizovaný instalátor a CLI).
