Na sekundárním trhu byla právě zakoupena následující participace:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Zbývá splátek:               ${data.loanTermRemaining?c} z ${data.loanTerm?c}
- Hodnota participace:         ${data.amountHeld?string.currency}
- Dosažitelný výnos*:          ${data.yield?string.currency} (${data.relativeYield?string.@interest} p. a.)

Dodatečné informace o půjčce:
- Účel:                        ${data.loanPurpose.getCode()?cap_first}
- Kraj:                        ${data.loanRegion.getCode()?cap_first}
- Zdroj příjmů:                ${data.loanMainIncomeType.getCode()?cap_first}
- Více na:                     ${data.loanUrl}

Nový zůstatek na Zonky účtu je ${data.newBalance?string.currency}.

<#include "warning-interest.ftl">
