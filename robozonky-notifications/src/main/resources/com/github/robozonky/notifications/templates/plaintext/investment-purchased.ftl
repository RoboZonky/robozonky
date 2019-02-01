Na sekundárním trhu byla právě zakoupena následující participace:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Úrok:                        <@idRating id=data.loanInterestRate />
- Zbývá splátek:               ${data.loanTermRemaining?c} z ${data.loanTerm?c}
- Hodnota participace:         ${data.amountHeld?string.currency}
- Dosažitelný výnos*:          ${data.yield?string.currency} (${data.relativeYield?string.@interest} p. a.)
- Záchranná vesta:             <#if data.insurance>Ano<#else>Ne</#if>.

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">

<#include "warning-interest.ftl">
