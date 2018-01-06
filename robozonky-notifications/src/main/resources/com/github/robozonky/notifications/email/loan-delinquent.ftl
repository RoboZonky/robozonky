Půjčka s následujícími parametry je v prodlení:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Původní délka splácení:      ${data.loanTerm?c} měsíců
- Původní dlužná částka:       ${data.loanAmount?string.currency}
- V prodlení od:               ${data.since?date}

Dodatečné informace o půjčce:
- Účel:                        ${data.loanPurpose.getCode()?cap_first}
- Kraj:                        ${data.loanRegion.getCode()?cap_first}
- Zdroj příjmů:                ${data.loanMainIncomeType.getCode()?cap_first}
- Více na:                     ${data.loanUrl}
