function renderTradeMenu(current) {
  var arr = [
    { id: 1, title: '交易登录', url: '/tradeConfig/login.html' },
    { id: 2, title: '交易规则', url: '/tradeConfig/ruleList.html' }
  ];

  renderMenu(arr, '.menu-nav', current);
}
