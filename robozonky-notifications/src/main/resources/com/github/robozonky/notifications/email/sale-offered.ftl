Participace s následujícími parametry byla právě vystavena na sekundární trh:

- Číslo půjčky:                ${data.loanId?c}
- Rating:                      ${data.loanRating}
- Zbývá splátek:               ${data.loanTermRemaining?c}
- Hodnota participace:         ${data.investedAmount?string.currency}

Informace o této půjčce jsou dostupné na následující adrese:
${data.loanUrl}
