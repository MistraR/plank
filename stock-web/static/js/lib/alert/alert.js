function Alert(message, action) {
  this.message = message;
  this.action = action;
}

Alert.prototype.show = function () {

  document.documentElement.style.overflow = 'hidden';
  this.showCover();
  this.showBody();
  this.showButton();
}

Alert.prototype.showCover = function () {
  var cover = document.createElement('div');
  cover.className = 'cover';
  document.getElementById("container").appendChild(cover);
}

Alert.prototype.showBody = function () {
  var box = document.createElement('div');
  box.className = 'box';
  document.getElementById("container").appendChild(box);
  var title = document.createElement('p');
  title.className = 'titleAlert';
  title.innerHTML = this.message;
  box.appendChild(title);
  var buttonBox = document.createElement('div');
  buttonBox.className = 'buttonBox';
  box.appendChild(buttonBox);

}

Alert.prototype.showButton = function () {
  var button = document.createElement('button');
  button.className = 'button';
  button.innerHTML = '确定';
  document.getElementsByClassName('buttonBox')[0].appendChild(button);
  var alert = this;

  button.onclick = function () {

    alert.close();

    if (typeof alert.action == 'function') {
      alert.action;
    }


  }
}

Alert.prototype.close = function () {

  var cover = document.querySelector('.cover');
  document.getElementById("container").removeChild(cover);

  var box = document.querySelector('.box');
  document.getElementById("container").removeChild(box);

  document.documentElement.style.overflow = 'auto';
}
