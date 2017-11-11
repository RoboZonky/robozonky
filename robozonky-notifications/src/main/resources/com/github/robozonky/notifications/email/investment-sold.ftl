Participace s následujícími parametry byla úspěšně prodána:

- Číslo půjčky:                ${data.loanId?c}
- Rating:                      ${data.loanRating}
- Zbývá splátek:               ${data.loanTermRemaining?c}
- Hodnota participace:         ${data.investedAmount?string.currency}

Informace o této půjčce jsou dostupné na následující adrese:
${data.loanUrl}

Nový zůstatek na Zonky účtu je ${data.newBalance?string.currency}.

