Půjčka s následujícími parametry byla zesplatněna:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Zbývá splatit:               ${data.amountRemaining?string.currency} z ${data.amountHeld?string.currency}
- Zbývá splátek:               ${data.loanTermRemaining?c} z ${data.loanTerm?c}
- Po splatnosti od:            ${data.since?date}
- Záchranná vesta:             <#if data.insurance>Ano<#else>Ne</#if>.
- Odklad splácení:             <#if data.postponed>Ano<#else>Ne</#if>.

<#include "additional-collections-info.ftl">

<#include "additional-loan-info.ftl">
