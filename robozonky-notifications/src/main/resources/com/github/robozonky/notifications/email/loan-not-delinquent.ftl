Půjčka s následujícími parametry nově není v prodlení:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Zbývá splatit:               ${data.amountRemaining?string.currency} z ${data.amountHeld?string.currency}
- Zbývá splátek:               ${data.loanTermRemaining?c} z ${data.loanTerm?c}
- Po splatnosti od:            ${data.since?date}

Dodatečné informace o půjčce:
- Účel:                        ${data.loanPurpose.getCode()?cap_first}
- Kraj:                        ${data.loanRegion.getCode()?cap_first}
- Zdroj příjmů:                ${data.loanMainIncomeType.getCode()?cap_first}
- Více na:                     ${data.loanUrl}

