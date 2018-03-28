Půjčka s následujícími parametry nově není v prodlení:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Rating:                      ${data.loanRating}
- Zbývá splatit:               ${data.amountRemaining?string.currency} z ${data.amountHeld?string.currency}
- Zbývá splátek:               ${data.loanTermRemaining?c} z ${data.loanTerm?c}
- Po splatnosti od:            ${data.since?date}

<#include "additional-collections-info.ftl">

<#include "additional-loan-info.ftl">

