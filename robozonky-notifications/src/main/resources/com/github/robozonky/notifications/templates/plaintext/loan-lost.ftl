Půjčka s následujícími parametry byla odepsána:

- Půjčka:                      #${data.loanId?c} ${data.loanName?cap_first}
- Úrok:                        ${data.loanInterestRate} % p.a.
- Ztraceno:                    ${data.amountRemaining?string.currency} z ${data.amountHeld?string.currency}

<#include "additional-loan-info.ftl">
