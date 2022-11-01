function Confirm(message, action1, action2) {


  Alert.call(this,message,action1);

  this.cancelAction = action2;
}

Confirm.prototype = Object.create(Alert.prototype);
Confirm.prototype.constructor = Confirm;

Confirm.prototype.showButton = function () {

  var leftBtn = document.createElement('button');
  leftBtn.className = 'leftBtn';
  leftBtn.innerHTML = '确定';
  document.querySelector('.buttonBox').appendChild(leftBtn);

  var rightBtn = document.createElement('button');
  rightBtn.className = 'rightBtn';
  rightBtn.innerHTML = '取消'
  document.querySelector('.buttonBox').appendChild(rightBtn);

  var confirm = this;

  leftBtn.onclick = function () {

    confirm.close();

    confirm.action();
  }

  rightBtn.onclick = function () {

    confirm.close();
    confirm.cancelAction();

  }
}
