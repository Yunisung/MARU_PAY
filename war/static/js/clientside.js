var MARU = (function (win, doc) {
  var c3Config = {
    payRoute: "regular",
    publicKey: '',    // 필수값
    amount: 0,        // 필수값
    trackId: '',
    responseFunction: '',
    redirectUrl: '',
    webhookUrl: '',
    widgetLogoUrl: '',
    mode: 'layer',
    debugMode: 'sandbox'
  }

  /* GLOBAL */
  var routeUrls = {
    sandbox: 'https://devapi.bkwinners.kr',
	  live: 'https://api.bkwinners.kr'
  }

  //api/echo를 clientside에서 사용하기위해 만듬
  var paykey = '';
  var echoSuccess;
  var echoFail;

  var routeDomain = routeUrls[c3Config.debugMode];
  var layerInited = false;            // 레이어 생성이 완료되었는지
  var layerLoaded = false;            // 레이어가 로드 되었는지
  var sendedIdArray = [];           // Iframe에서 전송받은 메시지의 id를 저장.
  var debug = false;
  var error = { code: '0000', message: '' }

  var util = {
    addEventListener: function (obj, event, fnc) {
      obj.addEventListener ? obj.addEventListener(event, fnc) : obj.attachEvent("on" + event, fnc);
    },
    removeEventListener: function (obj, event, fnc) {
      obj.removeEventListener ? obj.removeEventListener(event, fnc) : obj.detachEvent("on" + event, fnc);
    },
    documentReady: function (fnc) {
      if (document.addEventListener) {
        document.addEventListener("DOMContentLoaded", function () {
          document.removeEventListener("DOMContentLoaded", arguments.callee, false);
          fnc();
        }, false);
      } else if (document.attachEvent) {// Internet Explorer
        document.attachEvent("onreadystatechange", function () {
          if (document.readyState === "complete") {
            document.detachEvent("onreadystatechange", arguments.callee);
            fnc();
          }
        });
      }
    },
    createElementById: function (id, tag) {
      tag || (tag = 'div');
      var element = doc.createElement(tag);
      id && (element.id = id);
      return element;
    },
    getBrowserInfo: function () {
      var a = navigator.userAgent,
        b, d = a.match(/(opera|chrome|safari|firefox|msie|trident(?=\/))\/?\s*(\d+)/i) || []; if (/trident/i.test(d[1])) return b = /\brv[ :]+(\d+)/g.exec(a) || [], { name: "IE ", version: b[1] || "" }; if ("Chrome" === d[1] && (b = a.match(/\bOPR\/(\d+)/), null != b)) return { name: "Opera", version: b[1] };
      d = d[2] ? [d[1], d[2]] : [navigator.appName, navigator.appVersion, "-?"];
      null != (b = a.match(/version\/(\d+)/i)) && d.splice(1, 1, b[1]); return { name: d[0], version: d[1], mobile: util.isMobile() }
    },
    isIE: function () { var a = !1; "Microsoft Internet Explorer" == navigator.appName ? null !== /MSIE ([0-9]{1,}[\.0-9]{0,})/.exec(navigator.userAgent) && (a = parseFloat(RegExp.$1)) : "Netscape" == navigator.appName && ~navigator.appVersion.indexOf("Trident") && (a = 11); return a },
    isBrokenIE: function () { return !!util.isIE() && 10 > util.isIE() },
    isOldIE: function () { return !!util.isIE() && 7 >= util.isIE() },
    isIE8: function () { return !!util.isIE() && 8 == util.isIE() },
    isSafari: function () {
      var a = navigator.userAgent.toLowerCase(), b;
      - 1 != a.indexOf("safari") && -1 === a.indexOf("chrome") && (b = !0);
      util.isSafari = function () { return b };
      return util.isSafari()
    },
    numberWithCommas: function(x){ return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");},
    isRedirectBrowser: function () { return !!util.isIE() && 9 > util.isIE() },
    isMobile: function (a) { var b = navigator.userAgent; return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|SymbianOS|Mobile Safari/i.test(b) && (a || !(b.match(/Xoom|iPad/i) || b.match(/Nexus|Android/i) && (!b.match(/Mobile/i) || !1 === 600 > window.screen.availWidth))) },
    isTablet: function () { return util.isMobile(!0) },
    getDomain: function (a) { return a ? win.location.href : win.location.hostname },
    getProtocol: function () { return win.location.protocol },
    getLocale: function (a) { return doc.getElementsByTagName("html")[0].getAttribute("lang") || a && (navigator.language || navigator.browserLanguage) },
    noop: function () { },
    guid: function () { function s4() { return ((1 + Math.random()) * 0x10000 | 0).toString(16).substring(1); } return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4(); },
    getFormatDate: function () { var now = new Date(); return (now.getFullYear() + "-" + now.getMonth() + 1) + "-" + now.getDate() + " " + now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds() + ":" + now.getMilliseconds(); },
    validCreditCard: function(value) { if (/[^0-9-\s]+/.test(value)) return false;var nCheck = 0, nDigit = 0, bEven = false;value = value.replace(/\D/g, "");for (var n = value.length - 1; n >= 0; n--) {var cDigit = value.charAt(n),nDigit = parseInt(cDigit, 10);if (bEven) {if ((nDigit *= 2) > 9) nDigit -= 9;}nCheck += nDigit;bEven = !bEven;}return (nCheck % 10) == 0;},
    validateEmail: function(email) {var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;return re.test(email);},
    log: function(logMessage){ if(debug) console.log.apply(console, arguments); },
    validation: function (config) {
      // CASE 1: 토큰 && 키 가지고 있는 경우 두 값이 유효한 값이면 통화(다른값은 검사X).
      // CASE 2: 키 && 필수값을 가지고 있는 경우 값들이 유효한 값이면 통과. 
      // PS. 코드 및 메시지를 서버와 동기화 하여 가져올 것 + 따로 오브젝트로 관리할 것.
      if (config.publicKey === '' || config.publicKey == 'undefined') {
        error.code = '4001'; error.message = 'publicKey 필수값이 없습니다.';
        return false;
      }
      if(!config.amount) {
        error.code = '4002'; error.message = 'amount 필수값이 없습니다.';
        return false;
      }

      return true;
    },
    extend: function (obj, src) {
      for (var key in src) {
        if (src.hasOwnProperty(key)) obj[key] = src[key];
      }
      return obj;
    },
    indexOf: function(array, obj){
      for(var i=0; i<array.length; i++){
          if(array[i]==obj){
              return i;
          }
      }
      return -1;
    },
    postAjax: function (url, data, success) {
      // ActiveXObject를 이용한 방법은 사용할 수 없음. Flash 또는 IFrame을 이용한 전송방식을 구현 해야 함.
      var xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
      xhr.open('POST', url);
      xhr.onreadystatechange = function () {
        if (xhr.readyState > 3 && xhr.status == 200) { success(JSON.parse(xhr.responseText)); }
      };
      xhr.setRequestHeader("Accept", "application/json");
      xhr.setRequestHeader("Accept-Language", "ko_KR");
      xhr.setRequestHeader("Authorization", c3Config.publicKey);
      xhr.setRequestHeader("Content-Type", "application/json");
      xhr.send(data);
      return xhr;
    },
    getAjax: function (url, success, error) {
      var xhr = new XMLHttpRequest();
      if (!('withCredentials' in xhr)) xhr = new XDomainRequest(); // fix IE8/9
      xhr.open('GET', url);
      xhr.onload = success;
      xhr.onerror = error;
      xhr.send();
      return xhr;
    },
    sendMessageToFrame: function (obj) {
      // 응답을 받을 필요 없는 메시지
      if(obj.type == 'IE8_RESIZE') {
        sendPost(obj);
        return;
      }
      var time = 50;
      var id = setInterval(toFrame, time);
      var msgId = util.guid();
      obj.msgId = msgId;

      function toFrame() {
        console.log('SEND MESSAGE time: ',time);
        time += 50;
        if (time > 20000) { error.code = "XXXX", error.message = "PostMessage Send Fail"; alert('서버와 통신할 수 없습니다.'); clearInterval(id); }

        if(util.indexOf(sendedIdArray, msgId) > -1) {
          console.log('Clear Interval', id);
          clearInterval(id);
        } else {
          sendPost(obj);
        }
      }

      function sendPost(obj) {
        console.log('SEND POSTMESSAGE TO IFRAME:', obj, 'Message ID:', obj.msgId);
        var contentWindow = doc.getElementById("c3_pop_iframe").contentWindow;
        contentWindow.postMessage(JSON.stringify(obj), routeDomain);
      }

      /* setTimeout(function () {
        if (!layerLoaded) {
          console.log('SEND POSTMESSAGE TO IFRAME:', obj);
          var contentWindow = doc.getElementById("c3_pop_iframe").contentWindow;
          contentWindow.postMessage(JSON.stringify(obj), routeDomain);
        } else {
          util.sendMessageToFrame(obj, time);
        }
      }, time); */
    }
  }
  // IFrame에 보낼 메시지 정의
  var postMessages = {
    ie8Resize: function () {
      var obj = { type: 'IE8_RESIZE', width: document.documentElement.clientWidth, height: document.documentElement.clientHeight };
      util.sendMessageToFrame(obj);
    },
    payOpen: function () {
      var obj = { type: 'PAY_OPEN', c3Config: c3Config };
      util.sendMessageToFrame(obj);
    },
    payClose: function () {
      var obj = { type: 'PAY_CLOSE' };
      util.sendMessageToFrame(obj);
    },
    echo: function () {
      var obj = { type: 'ECHO', key: paykey };
      util.sendMessageToFrame(obj);
    }
  }
  // 레이어 팝업을 띄우기 위한 Element 생성 및 초기화. (로딩시)
  function layerInit() {
    if (layerInited) return
    layerInited = true
    var popOverlay = util.createElementById('c3_pop_overlay');
    popOverlay.setAttribute('style', 'position:fixed;top:0;bottom:0;left:0;right:0;background: rgba(0, 0, 0, 0.5);width:100%;height:100%;filter:progid:DXImageTransform.Microsoft.gradient(startColorstr=#60000000,endColorstr=#60000000);');
    var popOverlayWrap = util.createElementById('c3pop_pop_overlay_wrap');
    popOverlayWrap.setAttribute('style', 'z-index:2147483600;position:absolute;top:0;bottom:0;left:0;right:0;');
    var contentFixed = util.createElementById('c3pop_content_fixed');
    contentFixed.setAttribute('style', 'z-index: 2147483601;position:fixed;top: 0;left: 0;width:100%;height:100%;background-color:transparent;');
    var popCloseBtn = util.createElementById('c3pop_close_btn');
    popOverlayWrap.innerHTML += '<style>#c3pop_close_btn:hover {opacity: 1;} #c3pop_close_btn {position: absolute;  right: 20px;  top: 20px;  width: 20px; height: 20px; opacity: 0.5; color: white;} #c3pop_close_btn:before, #c3pop_close_btn:after {  position: absolute;  left: 8px;  content: \' \';  height: 20px;  width: 3px;  background-color: white;} #c3pop_close_btn:before {-webkit-transform: rotate(-45deg);-moz-transform: rotate(-45deg);-o-transform: rotate(-45deg);transform: rotate(-45deg);} #c3pop_close_btn:after {-webkit-transform: rotate(45deg);-moz-transform: rotate(45deg);-o-transform: rotate(45deg);transform: rotate(45deg);}</style>';
    var popIframe = util.createElementById('c3_pop_iframe', 'iframe');
    popIframe.setAttribute('frameborder', 0);
    popIframe.setAttribute('allowTransparency', true);
    popIframe.setAttribute('style', 'position:fixed;top: 0;left: 0;width:100%;height:100%;');

    popOverlayWrap.appendChild(popOverlay);
    contentFixed.appendChild(popIframe);
    contentFixed.appendChild(popCloseBtn);
    doc.body.appendChild(popOverlayWrap);
    doc.body.appendChild(contentFixed);

    popOverlayWrap.style.display = 'none';
    popOverlay.style.display = 'none';
    contentFixed.style.display = 'none';
    if (util.isIE8()) util.addEventListener(contentFixed, 'click', removePop); // 화면 클릭하면 닫히도록 수정 필요
    else util.addEventListener(popCloseBtn, 'click', removePop);
    util.addEventListener(win, 'keydown', function (e) {
      if (e.keyCode == 27) removePop();
    });
  }
  /* 레이어 팝업 실행 (Private) */
  function c3pop() {
    console.log('c3pop', routeDomain + '/form/payment/layout');
    doc.getElementById('c3_pop_iframe').src = routeDomain + '/form/payment/layout'; // 정해지면 변경...

    doc.getElementById('c3pop_pop_overlay_wrap').style.display = '';
    doc.getElementById('c3_pop_overlay').style.display = '';
    doc.getElementById('c3pop_content_fixed').style.display = '';
  }

  function removePop() {
    postMessages.payClose();
  }

  function layerClosed() {
    doc.getElementById('c3pop_pop_overlay_wrap').style.display = 'none';
    doc.getElementById('c3_pop_overlay').style.display = 'none';
    doc.getElementById('c3pop_content_fixed').style.display = 'none';
    
    doc.getElementById('c3_pop_iframe').src = 'about:blank';
  }

  function payResult (data) {
    var url = c3Config.redirectUrl;
    if (url && url.length > 1) {
      console.log('[CLIENT] REDIRECT: ', url);
      var form = doc.createElement('form');
      form.method = 'GET';
      form.action = url;
      var elem = doc.createElement('input');
      elem.value = JSON.stringify(data);
      elem.name = 'result';
      form.appendChild(elem);
      doc.body.appendChild(form);
      form.submit();
    }
    var resFnc = c3Config.responseFunction;
    if(resFnc && typeof resFnc == 'function') {
      resFnc(data);
    }
  }

  function echoResult(result) {
    if(result.resultCd === '0000') {
      echoSuccess(result);
    } else {
      echoFail();
    }
  }


  /*==== 실행 구문 ====*/
  function init() {
    util.log('IMPORT CLIENT.JS FILE');
    if (util.isOldIE()) {
      alert('지원하지 않는 브라우저 입니다.'); return;
    }
    layerInit();

    /* IFRAME 으로부터 이벤트를 전달 받는 부분. */
    util.addEventListener(window, 'message', function (e) {
      var recv = JSON.parse(e.data);
      console.log(recv);
      if (!recv.type) {
        console.log('undefined Type PostMessage');
      } else if (recv.type === 'LAYER_LOADED') {
        layerLoaded = true;
      } else if (recv.type === 'REVC_ACK') {
        sendedIdArray.push(recv.msgId);
      } else if (recv.type === 'LAYER_CLOSED') {
        layerClosed();
      } else if (recv.type === 'PAY_RESULT') {
        payResult(recv.data);
      } else if (recv.type === 'ECHO_RESULT') {
        echoResult(recv.data.result);
      }
    });

    if (util.isIE8()) {
      /* MSIE 8 버전은 화면 가운데 정렬이 불가능하여 해당 코드 실행 (서버 페이지로 사이즈 전송)*/
      var onresize = window.onresize;
      window.onresize = function (event) {
        if (typeof onresize === 'function') onresize();
        postMessages.ie8Resize();
      }
      /* 처음 실행 할 때도 Resize를 전송해서 사이즈 초기화 */
      setTimeout(function () {
        postMessages.ie8Resize();
      }, 1000);
    }
  }
  /* 결제 실행 (Public) */
  function pay(config) {
    c3Config = util.extend(c3Config, config);
    routeDomain = routeUrls[c3Config.debugMode];
    console.log('routeDomain', routeDomain);
    if(!c3Config.trackId) {
      c3Config.trackId = "TS" + (new Date().getTime() * (Math.floor(Math.random() * 10) +1));
    }
    if (!util.validation(config)) {
      alert('입력값이 올바르지 않아 결제를 진행할 수 없습니다.\n\n' + error.message + "(" + error.code + ")");
      return;
    }
    // if(util.isMobile()) {
    //   c3Config.mode = 'popup';
    // }
    requestOpen();
  }

  function requestOpen() {
    c3pop();
    setTimeout(function() {
      postMessages.payOpen();
    }, 200);
  }

  function echoPop() {
    console.log('c3pop', routeDomain + '/form/payment/layoutV3');
    doc.getElementById('c3_pop_iframe').src = routeDomain + '/form/payment/layoutV3'; // 정해지면 변경...
    doc.getElementById('c3pop_pop_overlay_wrap').style.display = 'none';
    doc.getElementById('c3_pop_overlay').style.display = 'none';
    doc.getElementById('c3pop_content_fixed').style.display = 'none';
  }

  function echo(key, success, fail) {
    echoPop();
    paykey = key;
    echoSuccess = success;
    echoFail = fail;

    console.log(paykey);
    setTimeout(function() {
      postMessages.echo();
    }, 200);
  }

  function setDebug(bool) {
    debug = bool;
  }

  var MARU = {
    util: util,
    debug: setDebug,
    c3pop: c3pop,
    removec3pop: removePop,
    pay: pay,
    echo: echo
  }

  util.documentReady(init)
  // util.addEventListener(doc, "load", init)
  window.MARU = MARU
  return MARU
})(window, document);