Participace s následujícími parametry byla úspěšně prodána:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Hodnota participace:         ${data.amountRemaining?string.currency}
- Doba držení:                 ${data.monthsElapsed?c} měsíců
- Dosažený výnos*:             ${data.yield?string.currency}

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">

<#include "warning-interest.ftl">
