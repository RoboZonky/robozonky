Půjčka s následujícími parametry byla zamítnuta:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Úrok:                        ${data.loanInterestRate} % p.a.
- Délka splácení:              ${data.loanTerm?c} měsíců
- Požadovaná částka:           ${data.loanAmount?string.currency}
- Navržená výše investice:     ${data.loanRecommendation?string.currency}

<#include "additional-loan-info.ftl">
