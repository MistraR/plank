Date.prototype.format = function(fmt) {
  var o = {
    "M+": this.getMonth() + 1, //月份
    "d+": this.getDate(), //日
    "h+": this.getHours(), //小时
    "m+": this.getMinutes(), //分
    "s+": this.getSeconds(), //秒
    "q+": Math.floor((this.getMonth() + 3) / 3), //季度
    "S": this.getMilliseconds() //毫秒
  };
  if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
  for (var k in o)
    if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
  return fmt;
};

if (String.prototype.startWith !== 'function') {
  String.prototype.startWith = function(str) {
    var reg = new RegExp("^" + str);
    return reg.test(this);
  }
}

if (String.prototype.endsWith !== 'function') {
  String.prototype.endsWith = function(str) {
    var reg = new RegExp(str + "$");
    return reg.test(this);
  }
}

var LocationUtil = {
  goto: function(url) {
    window.location = url;
  },
  open: function(url) {
    window.open(url);
  },
  getQueryString: function(name) {
    var reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)');
    var r = window.location.search.substr(1).match(reg);
    if (r != null) {
      return unescape(r[2]);
    }
    return null;
  }
};

var StorageUtil = {
  get: function(key) {
    var data = localStorage[key];
    if (typeof data == 'string') {
      try {
        var obj = JSON.parse(data);
        if (typeof obj === 'object' && obj) {
          var expire = obj.expire;
          if (expire && expire > new Date().getTime()) {
            return obj.value;
          }
        }
      } catch(e) {
      }
    }
    return '';
  },
  set: function(key, value) {
    var date = new Date();
    date.setTime(date.getTime() + 24 * 3600 * 1000);
    var expire = date.getTime();
    localStorage[key] = JSON.stringify({ expire, value });
  },
  remove: function(key) {
    localStorage.removeItem(key);
  }
};

var ExceptionHandler = {
  handleCommonError: function(xhr, location) {
    var message = xhr.responseJSON ? xhr.responseJSON.message : 'Internal Server Error';
    alert(message);
    if (location) {
      LocationUtil.goto(location);
    }
  }
};

function isBusinessTime(date) {
  if (!date) {
    date = new Date();
  }
  var hours = date.getHours();
  if (hours < 9 || hours >= 15 || hours === 12) {
    return false;
  }
  var minutes = date.getMinutes();
  return !(hours === 9 && minutes < 30 || hours === 11 && minutes > 30);
}
