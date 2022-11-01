var DataTable = function() {
  var PAGE_SIZE = 10;

  var _options;

  var init = function(options) {
    _options = options;
    frameInit();
    fnDraw();
  };

  function frameInit() {
    var src = getSrc(_options);

    var wrapper = $('<div class="table-list-wrapper"></div>');
    src.after(wrapper);

    src.appendTo(wrapper);

    src.after($('<div class="pagination-panel"></div>'));
  }

  function fnDraw(currentPage) {
    var src = getSrc(_options);
    var pageDom = src.next();
    var pageNumberInput = $('.data-table-page-number', pageDom);
    currentPage = currentPage || (pageNumberInput.length === 0 ? 1 : pageNumberInput.val());

    var start = (currentPage - 1) * PAGE_SIZE;

    var ajaxInParam = _options.dataTable.ajax;

    var data = ajaxInParam.data || {};
    data.start = start;
    data.length = PAGE_SIZE;

    var ajaxOptions = {
      url: ajaxInParam.url,
      type: ajaxInParam.type || 'GET',
      data: data,
      headers: ajaxInParam.headers || {},
      error: function(result) {
        if (ajaxInParam.error) {
          ajaxInParam.error(result);
        }
      },
      success: function(result) {
        render(result, currentPage, PAGE_SIZE);
      }
    };

    $.ajax(ajaxOptions);
  }

  function render(result, currentPage, pageSize) {
    var src = getSrc(_options);

    var dataTable = _options.dataTable;

    var dataSrc = dataTable['dataSrc'] || 'data';
    var columns = dataTable.columns;

    var data = result[dataSrc];
    var iTotalRecords = result['totalRecords'];

    src.empty();

    var thead = '<thead><tr>';
    var _fnColumnOptions = [];
    $.each(columns, function(key, column) {
      thead += '<th>' + column.title + '</th>';
      _fnColumnOptions[key] = getObjDataFn(column.render);
    });
    thead += '</tr></thead>';

    src.append(thead);

    var tbody = '<tbody>';
    for (var i = 0; i < data.length; i++) {
      var tr = '<tr>';
      for (var j = 0; j < _fnColumnOptions.length; j++) {
        tr += '<td>' + _fnColumnOptions[j](data[i]) + '</td>';
      }
      tr += '</tr>';
      tbody += tr;
    }
    tbody += '</tbody>';
    src.append(tbody);

    var pageBar = src.next();
    pageBar.html(renderPageBar(currentPage, result.totalRecords, pageSize));
    $('.data-table-page-number', pageBar).on('blur', function() {
      var tCurrentPage = parseInt($(this).val(), 10);
      var totalPage = getTotalPage(result.totalRecords, pageSize);
      if (tCurrentPage > totalPage) {
        $(this).val(totalPage);
        tCurrentPage = totalPage;
      } else if (tCurrentPage < 1) {
        $(this).val(1);
        tCurrentPage = 1;
      }
      if (tCurrentPage !== currentPage) {
        fnDraw();
      }
    });

    $('.pre', pageBar).on('click', function() {
      if (currentPage > 1) {
        $('.data-table-page-number', pageBar).val(parseInt(currentPage, 10) - 1);
        fnDraw();
      }
    });

    $('.next', pageBar).on('click', function() {
      var totalPage = getTotalPage(result.totalRecords, pageSize);
      if (currentPage < totalPage) {
        $('.data-table-page-number', pageBar).val(parseInt(currentPage, 10) + 1);
        fnDraw();
      }
    });

    if (_options.success) {
      _options.success(result);
    }

    if (dataTable.fnDrawCallback) {
      dataTable.fnDrawCallback(result);
    }

  }

  function getObjDataFn(fn) {
    return function(extra) {
      return fn(extra);
    };
  }

  function getSrc(options) {
    var src = options.src;
    if (!(src instanceof jQuery)) {
      src = $(src);
    }
    return src;
  }

  function renderPageBar(currentPage, iTotalRecords, pageSize) {
    var totalPage = getTotalPage(iTotalRecords, pageSize);
    return '<span class="pre">上一页</span><input class="data-table-page-number" type="number" min="1" step="1" value="' +
      currentPage + '" /><span class="next">下一页</span>,共' + totalPage + '页|每页显示' + pageSize  + '条记录|共' + iTotalRecords + '条记录';
  }

  function getTotalPage(iTotalRecords, pageSize) {
    return parseInt(iTotalRecords / pageSize, 10) + (iTotalRecords % pageSize === 0 ? 0 : 1);
  }

  return {
    init: init,
    fnDraw: fnDraw,
    setData: function(data) {
      _options.dataTable.ajax.data = data;
    }
  };
};
