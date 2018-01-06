Půjčka s následujícími parametry byla zesplatněna:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Zbývá splatit:               ${data.remainingAmount?string.currency} z ${data.totalAmount?string.currency}
- Zbývá splátek:               ${data.remainingMonths?c} z ${data.totalMonths?c}
- Po splatnosti od:            ${data.since?date}

Více o půjčce na ${data.loanUrl}.
