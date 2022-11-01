function renderTradeMenu(current) {
  var arr = [
    { id: 1, title: '定时任务', url: '/system/taskList.html' },
    { id: 2, title: '缓存', url: '/system/cacheList.html' },
    { id: 3, title: '系统配置', url: '/system/configList.html' }
  ];

  renderMenu(arr, '.menu-nav', current);
}
