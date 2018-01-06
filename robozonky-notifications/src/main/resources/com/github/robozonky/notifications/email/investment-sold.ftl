Participace s následujícími parametry byla úspěšně prodána:

- Číslo půjčky:                ${data.loanId?c}
- Rating:                      ${data.loanRating}
- Zbývá splátek:               ${data.loanTermRemaining?c}
- Hodnota participace:         ${data.investedAmount?string.currency}
- Dosažený výnos*:             ${data.yield?string.currency} (${data.relativeYield?string.@interest} p. a.)

Informace o této půjčce jsou dostupné na následující adrese:
${data.loanUrl}

Nový zůstatek na Zonky účtu je ${data.newBalance?string.currency}.

* Před zdaněním, odhad. Skutečná hodnota se může lišit v závislosti na momentální výši poplatků, zvolené metodě výpočtu
  a celé řadě dalších faktorů. Směrodatné údaje poskytuje výhradně investorský dashboard Zonky.
