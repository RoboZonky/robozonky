Půjčka s následujícími parametry byla předána k investování jiným nástrojem:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Úrok:                        <@idRating id=data.loanInterestRate />
- Délka splácení:              ${data.loanTerm?c} měsíců
- Požadovaná částka:           ${data.loanAmount?c},- Kč
- Navržená výše investice:     ${data.loanRecommendation?string.currency}
- Cílový nástroj:              ${data.confirmationProviderId}

<#include "additional-loan-info.ftl">

