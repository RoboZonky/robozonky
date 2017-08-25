Půjčka s následujícími parametry byla zamítnuta:

- Číslo půjčky:                 ${data.loanId?c}
- Rating:                       ${data.loanRating}
- Délka splácení:               ${data.loanTerm?c} měsíců
- Požadovaná částka:            ${data.loanAmount?string.currency}
- Navržená výše investice:      ${data.loanRecommendation?string.currency}

Informace o této půjčce jsou dostupné na následující adrese:
${data.loanUrl}

Zůstatek na Zonky účtu je ${data.newBalance?string.currency}.

