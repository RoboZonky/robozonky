Půjčka s následujícími parametry je v prodlení:

- Číslo půjčky:                ${data.loanId?c}
- Rating:                      ${data.loanRating}
- Délka splácení:              ${data.loanTerm?c} měsíců
- Dlužná částka:               ${data.loanAmount?c},- Kč
- V prodlení od:               ${data.since?date}

Informace o této půjčce jsou dostupné na následující adrese:
${data.loanUrl}
