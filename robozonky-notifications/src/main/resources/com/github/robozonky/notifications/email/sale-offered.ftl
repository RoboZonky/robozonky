Participace s následujícími parametry byla právě vystavena na sekundární trh:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Hodnota participace:         ${data.amountRemaining?string.currency}
- Zbývá splátek:               ${data.loanTermRemaining?c}

Dodatečné informace o půjčce:
- Účel:                        ${data.loanPurpose.getCode()?cap_first}
- Kraj:                        ${data.loanRegion.getCode()?cap_first}
- Zdroj příjmů:                ${data.loanMainIncomeType.getCode()?cap_first}
- Více na:                     ${data.loanUrl}
