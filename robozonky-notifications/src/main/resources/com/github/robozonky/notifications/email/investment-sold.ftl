Participace s následujícími parametry byla úspěšně prodána:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Zbývá splátek:               ${data.loanTermRemaining?c}
- Hodnota participace:         ${data.investedAmount?string.currency}
- Dosažený výnos*:             ${data.yield?string.currency} (${data.relativeYield?string.@interest} p. a.)

Dodatečné informace o půjčce:
- Účel:                        ${data.loanPurpose.getCode()?cap_first}
- Kraj:                        ${data.loanRegion.getCode()?cap_first}
- Zdroj příjmů:                ${data.loanMainIncomeType.getCode()?cap_first}
- Více na:                     ${data.loanUrl}

Nový zůstatek na Zonky účtu je ${data.newBalance?string.currency}.

<#include "warning-interest.ftl">
