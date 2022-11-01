const { proxyUrl, port } = require('./config.js');

var killTask = function(port, callback) {
  var cmd = process.platform == 'win32' ? 'netstat -ano' : 'ps aux';
  var linesplitor = process.platform == 'win32' ? '\r\n' : '\n';

  var exec = require('child_process').exec;
  exec(cmd, function(err, stdout, stderr) {
    if (err) { return console.log(err); }

    var arr = stdout.split(linesplitor);

    var pid = null;
    for (var i in arr) {
      var p = arr[i].trim().split(/\s+/);
      if (p.length === 5 && p[1].split(':')[1] == port) {
        pid = p[4];
        break;
      }
    }

    if (pid) {
      exec('taskkill /F /pid ' + pid, function(err) {
        if (err) { console.log('error {}', err); }
        if (callback) { callback(); }
      });
    } else {
      if (callback) { callback(); }
    }

  });

}

killTask(port, function() {
  var connect = require('connect');
  var serveStatic = require('serve-static');
  var proxy = require('proxy-middleware');

  var app = connect();

  app.use('/api', proxy(proxyUrl));
  app.use(serveStatic('.'));
  app.listen(port, function() {
    console.log("http://127.0.0.1:%s", port);
  });

});
