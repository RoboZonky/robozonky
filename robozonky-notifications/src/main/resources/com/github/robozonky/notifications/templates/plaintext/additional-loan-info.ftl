Dodatečné informace o půjčce:
- Účel:                        ${data.loanPurpose.getCode()?cap_first}
- Kraj:                        ${data.loanRegion.getCode()?cap_first}
- Zdroj příjmů:                ${data.loanMainIncomeType.getCode()?cap_first}
<#if data.investedOn??>
- Zainvestováno:               ${data.investedOn?date}
</#if>
- Více na:                     ${data.loanUrl}
