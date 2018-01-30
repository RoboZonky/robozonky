Půjčka s následujícími parametry byla zamítnuta:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Délka splácení:              ${data.loanTerm?c} měsíců
- Požadovaná částka:           ${data.loanAmount?string.currency}
- Navržená výše investice:     ${data.loanRecommendation?string.currency}

<#include "additional-loan-info.ftl">
