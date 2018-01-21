Půjčka s následujícími parametry byla zcela splacena:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Zaplaceno:                   ${data.totalPaid?string.currency} za půjčených ${data.loanAmount?string.currency}
- Počet splátek:               ${data.loanTermElapsed?c} z původních ${data.loanTerm?c}
- Dosažený výnos*:             ${data.yield?string.currency} (${data.relativeYield?string.@interest} p. a.)

Dodatečné informace o půjčce:
- Účel:                        ${data.loanPurpose.getCode()?cap_first}
- Kraj:                        ${data.loanRegion.getCode()?cap_first}
- Zdroj příjmů:                ${data.loanMainIncomeType.getCode()?cap_first}
- Více na:                     ${data.loanUrl}

Nový zůstatek na Zonky účtu je ${data.newBalance?string.currency}.

<#include "warning-interest.ftl">
