Participace s následujícími parametry byla právě vystavena na sekundární trh:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Hodnota participace:         ${data.amountRemaining?string.currency}
- Zbývá splátek:               ${data.loanTermRemaining?c}
- Záchranná vesta:             <#if data.insurance>Ano<#else>Ne</#if>.

<#include "additional-loan-info.ftl">
