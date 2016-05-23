# RoboZonky

_RoboZonky_ je [ke stažení](http://search.maven.org/remotecontent?filepath=com/github/triceo/robozonky/robozonky-app/1.0.3.Final/robozonky-app-1.0.3.Final-dist.zip).

## Jak to funguje

## Investiční strategie

Investiční strategie je soubor, ve kterém _RoboZonky_ řeknete, jaké složení investičního portfolia má mít za cíl. Na
ukázku má RoboZonky zabudované tři investiční strategie:
[vyváženou](robozonky-app/src/main/assembly/resources/robozonky-balanced.cfg),
[konzervativní](robozonky-app/src/main/assembly/resources/robozonky-conservative.cfg) a
[dynamickou](robozonky-app/src/main/assembly/resources/robozonky-dynamic.cfg). My si teď popíšeme
podrobněji tu dynamickou, jejíž pochopení by vám mělo umožnit napsat vlastní strategii přesně podle vašeho gusta.

### Cílový podíl ratingu na penězích ve hře

Klíčovou vlastností investiční strategie je podíl, který si přejete, aby investice daného ratingu ve vašem portfoliu
měly. _RoboZonky_ jej počítá tak, že vezme všechny vaše peníze, které v dané chvíli máte ve hře, rozdělí je podle 
ratingů a vydělí toto číslo součtem investic ve hře. V souboru investiční strategie se to vyjadřuje následujícím 
způsobem:

Řádek `targetShare.AA = 0.15` říká, že investice s ratingem A+ by měly zastávat 15 % portfolia. Stejným způsobem 
můžete nastavit výsledný podíl pro všechny ostatní ratingy (AAAAA, AAAA, AAA, AA, A, B, C, D). Řádek
`targetShare.default = 0.20` potom říká, že každý takto nenastavený rating má v portfoliu zastávat 20 %. Tento řádek 
není povinný, uvedete-li zvláštní řádek pro každý rating.

_RoboZonky_ umožňuje také přeinvestování. Pokud bude součet všech cílových podílů větší než 1, použijí se totiž 
stejná pravidla jako výše. Pokud si tedy nastavíte např. cílový podíl investic pro rating A** na 60 %, a na tržišti 
bude zrovna dostatek půjček s ratingem A** a přitom žádné jiné, RoboZonky začne ve velkém hrnout peníze do ratingu 
A**. To může být v některých situacích žádané chování, nicméně já doporučuji držet součet všech cílových podílů velmi
blízko jedničky a vyhnout se tak nepříjemným překvapením.

### Největší a nejmenší akceptovatelná délka splácení

Pro každý rating máte v investiční strategii možnost nastavit, jaká je nejmenší a největší možná doba splácení v
měsících, kterou jste schopní akceptovat.

`minimumTerm.default = 0` například říká, že pro všechny ratingy jinak neuvedené je minimální doba splácení 0 měsíců.
Jinými slovy - budou akceptovány i půjčky s nejkratší možnou dobou splatnosti.

`maximumTerm.AAA = 36` naopak říká, že v ratingu A++ mají být investovány pouze půjčky s dobou splácení do 36 měsíců.
Pro ratingy jinak neuvedené potom slouží řádek `maximumTerm.default = -1`, který říká, že mají být investovány půjčky 
s libovolně dlouhou dobou splácení.

### Výše investice do jednotlivé půjčky

_RoboZonky_ umožňuje ve strategii nakonfigurovat maximální velikost investice, a to dvěma způsoby. Jednak uvedením 
absolutní maximální částky, kterou může do jedné půjčky investovat. Druhak uvedením maximálního podílu, který vaše 
investice může činit na celkové výši úveru.

Obě tato kritéria se převedou do částky, kterou RoboZonky investuje do úvěrů, takto:
* Vezměte v úvahu úvěr o výši 100.000 Kč s ratingem A*. V peněžence máte 330 Kč.
* Strategie definuje maximální podíl na úvěru 1 %. Maximální investice tedy činí 1000 Kč.
* Strategie ale zároveň definuje absolutní maximální velikost investice, a to 400 Kč.
* Strategie vezme obě tyto maximální částky, a použije tu menší z nich.
* Do úvěru by tedy mělo být investováno 400 Kč.
* Nicméně v peněžence máte pouze 330 Kč, částka bude příslušně snížena zaokrouhlením na celé dvoustovky.
* Ve výsledku tedy bude investováno 200 Kč, 130 Kč zbývá v peněžence.

Tato kritéria se ve strategii definují následujícím způsobem:
* `maximumLoanShare.AAAA = 0.1` říká, že maximální podíl na investici s ratingem A* smí být 10 %.
* `maximumLoanAmount.default = 400` říká, že maximální částka pro jakoukoliv investici je 400 Kč.

_RoboZonky_ nikdy nepřekročí při investování zadanou absolutní maximální částku. Pokud by se snad stalo, že maximální 
výše investice vyjde nižší než 200 Kč - tj. menší než nejmenší možná investice na Zonky - nebude RoboZonky do takového 
úvěru investovat.

### Preferovaná délka doby splácení

Řekněme, že _RoboZonky_ našel na tržišti dvě půjčky nějakého konkrétního ratingu a nyní rozhoduje, do které z nich
investovat první. Řádek `preferLongerTerms.default = true` způsobí, že _RoboZonky_ jako první investuje do té půjčky, 
která má delší dobu splácení. Řádek `preferLongerTerms.default = false` bude mít za důsledek pravý opak.

A to je vše, vážení investoři! S takhle definovanou strategií už můžete pustit _RoboZonky_ na tržiště a vesele jej 
nechat rozhodovat za vás. Hodně štěstí!

## Proč to dělám

Nabízí se otázka, proč dávám _RoboZonky_ k dispozici zdarma a zbavuji se tak své "konkurenční výhody." Je to 
jednoduché - jsem motivován snahou vyrovnat startovní pole. Podle mého názoru s existencí všeobecně dostupných robotů 
nastane jedna z několika věcí:
* Zonky přivede na tržiště více úroků s žádanými ratingy, a roboti přestanou být potřeba.
* Každý bude mít robota, a všichni tak budou mít stejné šance.
* Zonky roboty zakáže, a všichni tak budou mít stejné šance.

Každá z těchto možností je vítaná, a _RoboZonky_ je můj příspěvek k jejich naplnění.
