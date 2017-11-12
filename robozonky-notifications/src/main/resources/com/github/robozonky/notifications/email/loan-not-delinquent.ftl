Půjčka s následujícími parametry nově není v prodlení:

- Číslo půjčky:                ${data.loanId?c}
- Rating:                      ${data.loanRating}
- Původní délka splácení:      ${data.loanTerm?c} měsíců
- Původní dlužná částka:       ${data.loanAmount?string.currency}
- Po splatnosti od:            ${data.since?date}

Informace o této půjčce jsou dostupné na následující adrese:
${data.loanUrl}
