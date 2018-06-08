Půjčka s následujícími parametry byla zcela splacena:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Zaplaceno:                   ${data.amountPaid?string.currency} za půjčených ${data.amountHeld?string.currency}
- Doba držení:                 ${data.monthsElapsed?c} měsíců
- Dosažený výnos*:             ${data.yield?string.currency}

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">

<#include "warning-interest.ftl">
