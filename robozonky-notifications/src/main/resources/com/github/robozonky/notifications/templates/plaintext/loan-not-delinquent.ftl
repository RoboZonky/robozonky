Půjčka s následujícími parametry nově není v prodlení:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Úrok:                        ${data.loanInterestRate} % p.a.
- Zbývá splatit:               ${data.amountRemaining?string.currency} z ${data.amountHeld?string.currency}
- Zbývá splátek:               ${data.loanTermRemaining?c} z ${data.loanTerm?c}
- Záchranná vesta:             <#if data.insurance>Ano<#else>Ne</#if>.
- Odklad splácení:             <#if data.postponed>Ano<#else>Ne</#if>.

<#include "additional-loan-info.ftl">

