Půjčka s následujícími parametry byla zesplatněna:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Úrok:                        <@idRating id=data.loanInterestRate />
- Zbývá splatit:               ${data.amountRemaining?string.currency} z ${data.amountHeld?string.currency}
- Zbývá splátek:               ${data.loanTermRemaining?c} z ${data.loanTerm?c}
- Po splatnosti od:            <@date data.since />
- Záchranná vesta:             <#if data.insurance>Ano<#else>Ne</#if>.

<#include "additional-loan-info.ftl">
