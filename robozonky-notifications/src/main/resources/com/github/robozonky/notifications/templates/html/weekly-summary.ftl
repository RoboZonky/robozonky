<table style="width: 60%">
  <caption>Výkonnost portfolia</caption>
  <thead>
    <tr>
        <th></th>
        <th style="padding: 5px; text-align: right;">Očekáváno</th>
        <th style="padding: 5px; text-align: right;">Udáváno <@zonky /></th>
        <th style="padding: 5px; text-align: right;">Ideální</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th style="padding: 5px; text-align: right;">Výnos</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.minimalProfitability?string.@interest} p.a.</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.profitability?string.@interest} p.a.</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.optimalProfitability?string.@interest} p.a.</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;">Měsíčně</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.minimalMonthlyProfit?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.monthlyProfit?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.portfolio.optimalMonthlyProfit?string.currency}</th>
    </tr>
  </tbody>
</table>

<#include "additional-portfolio-info.ftl">

<table style="width: 60%">
  <caption>Peněžní toky</caption>
  <thead>
    <tr>
        <th></th>
        <th style="padding: 5px;"></th>
        <th style="padding: 5px; text-align: right;">K Vám</th>
        <th style="padding: 5px; text-align: right;">Od Vás</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th style="padding: 5px; text-align: right;">Celkem</th>
      <th style="padding: 5px; text-align: right;">${data.inTotal?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.outTotal?string.currency}</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;">Pouze převody</th>
      <th style="padding: 5px; text-align: right;">${data.inFromDeposits?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.outFromWithdrawals?string.currency}</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;">Pouze poplatky</th>
      <th colspan="2" style="padding: 5px; text-align: right;">${data.outFromFees?string.currency}</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;"><strong>Výsledek</strong></th>
      <th colspan="2" style="padding: 5px; text-align: right;"><strong>${data.total?string.currency}</strong></th>
    </tr>
  </tbody>
</table>

<table style="width: 60%">
  <caption>Změny v portfoliu</caption>
  <thead>
    <tr>
        <th style="padding: 5px; width: 50%;">Nové investice (vč. nákupů)</th>
        <th style="padding: 5px; width: 50%;">Ukončené investice (vč. prodejů)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th>
        <table style="width: 100%">
            <#list data.incomingInvestments as item>
                <tr>
                    <th style="padding: 1px; text-align: right;">${item.amountHeld?string.currency}</th>
                    <th style="padding: 1px; text-align: right;"><@idRating id=item.loanInterestRate /></th>
                    <th style="padding: 1px; text-align: right;">${item.loanTerm?c} m.</th>
                    <th style="padding: 1px; text-align: center;"><@idLoan data=item /></th>
                </tr>
            </#list>
        </table>
      </th>
      <th>
        <table style="width: 100%">
            <#list data.outgoingInvestments as item>
                <tr>
                    <th style="padding: 1px; text-align: right;">${item.amountHeld?string.currency}</th>
                    <th style="padding: 1px; text-align: right;"><@idRating id=item.loanInterestRate /></th>
                    <th style="padding: 1px; text-align: right;">${item.loanTerm?c} m.</th>
                    <th style="padding: 1px; text-align: center;"><@idLoan data=item /></th>
                </tr>
            </#list>
        </table>
      </th>
    </tr>
  </tbody>
</table>
