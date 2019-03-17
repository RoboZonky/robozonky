Půjčka s následujícími parametry byla potvrzena v rezervačním systému:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Úrok:                        <@idRating id=data.loanInterestRate />
- Délka splácení:              ${data.loanTerm?c} měsíců
- Investovaná částka:          ${data.amountHeld?string.currency}
- Záchranná vesta:             <#if data.insurance>Ano<#else>Ne</#if>.

<#include "additional-loan-info.ftl">

<#include "additional-portfolio-info.ftl">
