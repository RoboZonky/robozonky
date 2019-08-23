Participace s následujícími parametry byla úspěšně prodána:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Úrok:                        <@idRating id=data.loanInterestRate />
- Hodnota participace:         ${data.amountRemaining?string.currency}
- Doba držení:                 ${data.monthsElapsed?c} měsíců
- Záchranná vesta:             <#if data.insurance>Ano<#else>Ne</#if>.

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">

Tato notifikace může mít za skutečným prodejem značné zpoždění.
