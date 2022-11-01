function renderTradeMenu(current) {
  var arr = [
    { id: 1, title: '我的持仓', url: '/trade/stockList.html' },
    { id: 2, title: '我的委托', url: '/trade/orderList.html' },
    { id: 3, title: '我的成交', url: '/trade/dealList.html' },
    { id: 4, title: '历史成交', url: '/trade/hisDealList.html' },
    { id: 5, title: '银证转账', url: '/trade/transfer.html', state: 0 }
  ];

  renderMenu(arr, '.menu-nav', current);
}
