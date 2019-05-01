<table style="width: 60%">
  <caption><h2>Výkonnost portfolia</h2></caption>
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
  <caption><h2>Peněžní toky</h2></caption>
  <thead>
    <tr>
        <th></th>
        <th style="padding: 5px; text-align: right;">K Vám</th>
        <th style="padding: 5px; text-align: right;">Od Vás</th>
        <th style="padding: 5px; text-align: right;">Výsledek</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th style="padding: 5px; text-align: right;">Celkem</th>
      <th style="padding: 5px; text-align: right;">${data.inTotal?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.outTotal?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.total?string.currency}</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;">Pouze převody</th>
      <th style="padding: 5px; text-align: right;">${data.inFromDeposits?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.outFromWithdrawals?string.currency}</th>
      <th style="padding: 5px; text-align: right;">${data.totalDepositsAndWithdrawals?string.currency}</th>
    </tr>
    <tr>
      <th style="padding: 5px; text-align: right;">Pouze poplatky</th>
      <th style="padding: 5px; text-align: right;">0,00 Kč</th>
      <th style="padding: 5px; text-align: right;">${data.outFromFees?string.currency}</th>
      <th style="padding: 5px; text-align: right;"><#if data.outFromFees < 0>-</#if>${data.outFromFees?string.currency}</th>
    </tr>
  </tbody>
</table>

<table style="width: 60%">
  <caption><h2>Změny v portfoliu</h2></caption>
  <thead>
    <tr>
        <th style="padding: 5px; width: 50%;">Nové investice (vč. nákupů)</th>
        <th style="padding: 5px; width: 50%;">Ukončené investice (vč. prodejů)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th>
        <#list data.incomingInvestments>
            <table style="width: 100%">
                <#items as item>
                    <tr>
                        <th style="padding: 1px; text-align: right;">${item.amountHeld?string.currency}</th>
                        <th style="padding: 1px; text-align: right;"><@idRating id=item.loanInterestRate /></th>
                        <th style="padding: 1px; text-align: right;">${item.loanTerm?c} m.</th>
                        <th style="padding: 1px; text-align: left;"><@idLoan data=item /></th>
                    </tr>
                </#items>
            </table>
        <#else>
            Žádné.
        </#list>
      </th>
      <th>
        <#list data.outgoingInvestments>
            <table style="width: 100%">
                <#items as item>
                    <tr>
                        <th style="padding: 1px; text-align: right;">${item.amountHeld?string.currency}</th>
                        <th style="padding: 1px; text-align: right;"><@idRating id=item.loanInterestRate /></th>
                        <th style="padding: 1px; text-align: right;">${item.loanTerm?c} m.</th>
                        <th style="padding: 1px; text-align: left;"><@idLoan data=item /></th>
                    </tr>
                </#items>
            </table>
        <#else>
            Žádné.
        </#list>
      </th>
    </tr>
  </tbody>
</table>
