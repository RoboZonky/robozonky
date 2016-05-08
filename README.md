# ZonkyBot

## Jak to funguje

## Investiční strategie

Investiční strategie je soubor, ve kterém ZonkyBotu řeknete, jaké složení investičního portfolia má mít za cíl. Na
ukázku má ZonkyBot zabudované tři investiční strategie:
[vyváženou](blob/master/zonkybot-app/src/main/assembly/resources/zonkybot-balanced.cfg),
[konzervativní](blob/master/zonkybot-app/src/main/assembly/resources/zonkybot-conservative.cfg) a
[dynamickou](blob/master/zonkybot-app/src/main/assembly/resources/zonkybot-dynamic.cfg). My si teď popíšeme
podrobněji tu dynamickou, jejíž pochopení by vám mělo umožnit napsat vlastní strategii přesně podle vašeho gusta.

### Cílový podíl ratingu na penězích ve hře

Klíčovou vlastností investiční strategie je podíl, který si přejete, aby investice daného ratingu ve vašem portfoliu
měly. ZonkyBot jej počítá tak, že vezme všechny vaše peníze, které v dané chvíli máte ve hře, rozdělí je podle ratingů a
vyděli toto číslo součtem investic ve hře. V souboru investiční strategie se to vyjadřuje následujícím způsobem:

Řádek `targetShare.AA = 0.15` říká, že investice s ratingem A+ by měly zastávat 15 % portfolia. Stejným způsobem můžete
nastavit výsledný podíl pro všechny ostatní ratingy (AAAAA, AAAA, AAA, AA, A, B, C, D). Řádek
`targetShare.default = 0.20` potom říká, že každý takto nastavený rating má v portfoliu zastávat 20 %. Tento řádek není
povinný, uvedete-li zvláštní řádek pro každý rating.

### Největší a nejmenší akceptovatelná délka splácení

Pro každý rating máte v investiční strategii možnost nastavit, jaká je nejmenší a největší možná doba splácení v
měsících, kterou jste schopní akceptovat.

`minimumTerm.default = 0` například říká, že pro všechny ratingy jinde neuvedené je minimální doba splácení 0 měsíců.
Jinými slovy - budou akceptovány i půjčky s nejkratší možnou dobou splatnosti.

`maximumTerm.AAA = 36` naopak říká, že v ratingu A++ mají být investovány pouze půjčky s dobou splatnosti do 36 měsíců.
Pro ratingy jinak neuvedené potom slouží řádek `maximumTerm.default = -1`, který říká, že mají být investovány půjčky s
libovolně dlouhou splatností.

### Výše investice do jednotlivé půjčky

ZonkyBot umožňuje ve strategii nakonfigurovat maximální velikost půjčky, a to dvěma způsoby. Jednak uvedením absolutní
maximální částky, kterou může do jedné půjčky investovat. Druhak uvedením maximálního podílu, který vaše investice může
činit na celkové výši úveru.

Obě tato kritéria se převedou do částky, kterou ZonkyBot investuje do úvěrů, takto:
* Vezměte v úvahu úvěr o výši 100.000 Kč s ratingem B. V peněžence máte 330 Kč.
* Strategie definuje maximální podíl na úvěru 1 %. Maximální investice tedy činí 1000 Kč.
* Strategie ale zároveň definuje absolutní maximální velikost investice, a to 400 Kč.
* Strategie vezme obě tyto maximální částky, a použije tu menší z nich.
* Do úvěru by tedy mělo být investováno 400 Kč.
* Nicméně v peněžence máte pouze 330 Kč, částka bude příslušně snížena zaokrouhlením na celé stovky.
* Ve výsledku tedy bude investováno 300 Kč, 30 Kč zbývá v peněžence.

Tato kritéria se ve strategii definují následujícím způsobem:
* `maximumLoanShare.AAAA = 0.1` říká, že maximální podíl na investici s ratingem A* smí být 10 %.
* `maximumLoanAmount.default = 400` říká, že maximální částka pro jakoukoliv investici je 400 Kč.

### Preferovaná délka doby splácení

Řekněme, že ZonkyBot našel na tržišti dvě půjčky nějakého konkrétního ratingu a nyní rozhoduje, do které z nich
investovat první. Řádek `preferLongerTerms.default = true` způsobí, že ZonkyBot jako první investuje do té půjčky, která
má delší dobu splácení. Řádek `preferLongerTerms.default = false` bude mít za důsledek pravý opak.

A to je vše, vážení investoři! S takhle definovanou strategií už můžete pustit ZonkyBota na tržiště a vesele jej nechat
rozhodovat za vás. Hodně štěstí!

