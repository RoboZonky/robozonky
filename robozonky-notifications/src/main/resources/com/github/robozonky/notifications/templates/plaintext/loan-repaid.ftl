Půjčka s následujícími parametry byla zcela splacena:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Úrok:                        <@idRating id=data.loanInterestRate />
- Zaplaceno:                   ${data.amountPaid?string.currency} za půjčených ${data.amountHeld?string.currency}
- Doba držení:                 ${data.monthsElapsed?c} měsíců

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">
