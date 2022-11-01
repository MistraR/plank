function renderTradeMenu(current) {
  var arr = [
    { id: 1, title: '我的持仓', url: '/crTrade/stockList.html' },
    { id: 2, title: '我的委托', url: '/crTrade/orderList.html' },
    { id: 3, title: '我的成交', url: '/crTrade/dealList.html' },
    { id: 4, title: '历史成交', url: '/crTrade/hisDealList.html' }
  ];

  renderMenu(arr, '.menu-nav', current);
}
