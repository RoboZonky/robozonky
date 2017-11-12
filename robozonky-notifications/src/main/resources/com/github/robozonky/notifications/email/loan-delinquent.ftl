Půjčka s následujícími parametry je v prodlení:

- Číslo půjčky:                ${data.loanId?c}
- Rating:                      ${data.loanRating}
- Původní délka splácení:      ${data.loanTerm?c} měsíců
- Původní dlužná částka:       ${data.loanAmount?string.currency}
- V prodlení od:               ${data.since?date}

Informace o této půjčce jsou dostupné na následující adrese:
${data.loanUrl}
