Participace s následujícími parametry byla úspěšně prodána:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Úrok:                        ${data.loanInterestRate} % p.a.
- Hodnota participace:         ${data.amountRemaining?string.currency}
- Doba držení:                 ${data.monthsElapsed?c} měsíců
- Dosažený výnos*:             ${data.yield?string.currency}
- Záchranná vesta:             <#if data.insurance>Ano<#else>Ne</#if>.

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">

<#include "warning-interest.ftl">
