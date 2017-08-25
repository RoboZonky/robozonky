Půjčka s následujícími parametry byla předána k investování jiným nástrojem:

- Číslo půjčky:                ${data.loanId?c}
- Rating:                      ${data.loanRating}
- Délka splácení:              ${data.loanTerm?c} měsíců
- Požadovaná částka:           ${data.loanAmount?c},- Kč
- Navržená výše investice:     ${data.loanRecommendation?string.currency}
- Cílový nástroj:              ${data.confirmationProviderId}

Informace o této půjčce jsou dostupné na následující adrese:
${data.loanUrl}

Zůstatek na Zonky účtu je ${data.newBalance?string.currency}.

