var KWON = (function (win, doc) {
	var c3Config = {
	    publicKey: '',    // 필수값
	    trackId: '',	  // 필수값
		authKey: '',
	    responseFunction: '',
	    redirectUrl: '',
	    webhookUrl: '',
	    mode: 'layer',
	    debugMode: 'test',
	    identity: '',
	    phoneNo: '',
	    authId: '',
	    holderName: '',
	    resultCd: '',
	    resultMsg: '',
	    advanceMsg: '',
	    create: '',
	    totalAuthId: '',
        bankCd: '',
		account: '',
        amount: '',
        oper: '',
		companyName: ''
	}

    var MaruConfig = {
	    publicKey: '',    // 필수값
	    trackId: '',	  // 필수값
		authKey: '',
	    responseFunction: '',
	    redirectUrl: '',
	    webhookUrl: '',
	    mode: 'layer',
	    debugMode: 'test',
	    identity: '',
	    phoneNo: '',
	    authId: '',
	    holderName: '',
	    resultCd: '',
	    resultMsg: '',
	    advanceMsg: '',
	    create: '',
	    totalAuthId: '',
        bankCd: '',
		account: '',
        amount: '',
        oper: '',
		companyName: ''
	}

	/* GLOBAL */
	var routeUrls = {
		test: 'https://svcapidev.mtouch.com',
		live: 'https://svcapi.mtouch.com'
	}
	//부국위너스 URL주소
	// var maruUrl = 'http://127.0.0.1:10002'; //local
	// var maruUrl = 'https://devapi.bkwinners.kr'; //dev
	var maruUrl = 'https://api.bkwinners.kr'; //live

	var maruUrls = {
		test: 'https://devapi.bkwinners.kr',
		live: 'https://api.bkwinners.kr'
	}


  	var routeDomain = routeUrls[c3Config.debugMode];
	var maruDomain = maruUrls[c3Config.debugMode];
  	var layerInited = false;
  	var layerLoaded = false;
  	var sendedIdArray = [];
  	var debug = false;
  	var error = { code: '0000', message: '' }

    var sendVactData = { 
        result: '',
        auth: '',
        vact: '',
        publicKey: ''
    }

	function MaruResponseFunction(data) {
		//광원인증후 결과값이 여기로 들어온다.
		//여기 데이터를 이용해 부국서버와 통신
		var vact = {
			bankCd: MaruConfig.bankCd,
			account: MaruConfig.account,
			amount: MaruConfig.amount,
			oper: MaruConfig.oper,
			holderName: MaruConfig.holderName,
			phoneNo: MaruConfig.phoneNo,
			identity: MaruConfig.identity,
			companyName: MaruConfig.companyName
		}
		sendVactData = {result: data.result, auth: data.auth, vact: vact, publicKey : MaruConfig.publicKey};
		console.log('sendData : ', sendVactData);
		console.log(JSON.stringify(sendVactData));

		// var jsonVactData = JSON.stringify(sendVactData);
		// console.log('-----------------');
		// console.log(jsonVactData.publicKey);
		// console.log('-----------------');


		//serverside.js로 데이터 보내는 처리
		requestSendVact();


	}

	function requestSendVact() {
		maruPop();

		setTimeout(function() {
			postMessages.sendVact();
		}, 200);
	}

	function maruPop() {
		console.log('c3pop', maruUrl + '/form/payment/vact/authVactLayout');

		doc.getElementById('c3_pop_iframe').src = maruUrl + '/form/payment/vact/authVactLayout';
		doc.getElementById('c3pop_pop_overlay_wrap').style.display = '';
		doc.getElementById('c3_pop_overlay').style.display = '';
		doc.getElementById('c3pop_content_fixed').style.display = '';
	}

	function onlyAuth(config) {
        //rediectURL, publicKey, responseFunction 따로저장
        //MARUConfig는 부국위너스 서버에 보낼 데이터 세팅 > 사용자가 입력한 값을 그대로 보내줌
        //c3Config는 광원서버에 보낼 데이터 세팅 > 광원에 등록된 부국위너스 publicKey를 사용 > 인증과정 거친후 return값을 부국위너스로 보냄
        
    	c3Config = util.extend(c3Config, config);
        MaruConfig = util.extend(MaruConfig, config);
        //c3Config.publicKey = 'pk_55af-b88fa5-8cb-6ff54';
		c3Config.publicKey = config.authKey;
        c3Config.redirectUrl = '';
        c3Config.responseFunction = MaruResponseFunction;
    	routeDomain = routeUrls[c3Config.debugMode];
    
    	console.log('routeDomain', routeDomain);
    
		if (!util.validation(config)) {
      		alert('입력값이 올바르지 않아 결제를 진행할 수 없습니다.\n\n' + error.message + "(" + error.code + ")");
      		return;
    	}

		requestOpen();
	}

	function requestOpen() {
		c3pop();

		setTimeout(function() {
			postMessages.authOpen();
		}, 200);
	}

	function c3pop() {
		console.log('c3pop', routeDomain + '/form/payment/onlyAuthLayout');

		doc.getElementById('c3_pop_iframe').src = routeDomain + '/form/payment/onlyAuthLayout';
		doc.getElementById('c3pop_pop_overlay_wrap').style.display = '';
		doc.getElementById('c3_pop_overlay').style.display = '';
		doc.getElementById('c3pop_content_fixed').style.display = '';
	}

	var postMessages = {
		ie8Resize: function () {
			var obj = { type: 'IE8_RESIZE', width: document.documentElement.clientWidth, height: document.documentElement.clientHeight };
			util.sendMessageToFrame(obj);
		},
		authOpen: function () {
			var obj = { type: 'AUTH_OPEN', c3Config: c3Config };
			util.sendMessageToFrame(obj);
		},
		authClose: function () {
			var obj = { type: 'AUTH_CLOSE' };
			util.sendMessageToFrame(obj);
		},
		sendVact: function() {
			var obj = {type: 'SEND_VACT', data: sendVactData};
			util.sendMessageToFramebyMaru(obj);
		}
	}

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

			if (config.publicKey === '' || config.publicKey == 'undefined') {
				error.code = '4001'; error.message = 'publicKey 필수값이 없습니다.';
				return false;
			}
			if(!config.trackId) {
				error.code = '4002'; error.message = 'trackId 필수값이 없습니다.';
				return false;
			}
			
			if(!config.holderName) {
				error.code = '4002'; error.message = 'holderName 필수값이 없습니다.';
				return false;
			}
			if(!config.phoneNo) {
				error.code = '4002'; error.message = 'phoneNo 필수값이 없습니다.';
				return false;
			}else{
				var regPhone = /^01([0|1])-?([0-9]{3,4})-?([0-9]{4})$/;
				var tempPhoneNo = config.phoneNo.replace(/(^02.{0}|^01.{1}|[0-9]{3})([0-9]+)([0-9]{4})/,"$1-$2-$3");

				if (regPhone.test(tempPhoneNo) === true) {
				} else {
					error.code = '4002'; error.message = '휴대폰번호가 올바르지 않습니다.';
					return false;
				}
			}
			// if(!config.redirectUrl) {
			// 	error.code = '4002'; error.message = 'redirectUrl 필수값이 없습니다.';
			// 	return false;
			// }
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

				if (time > 20000) {
					error.code = "XXXX", error.message = "PostMessage Send Fail";
					alert('서버와 통신할 수 없습니다.'); clearInterval(id);
				}

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
		},
		sendMessageToFramebyMaru: function (obj) {
			if(obj.type == 'IE8_RESIZE') {
				sendPostMaru(obj);
				return;
			}

			var time = 50;
			var id = setInterval(toFrameMaru, time);
			var msgId = util.guid();
			obj.msgId = msgId;

			function toFrameMaru() {
				console.log('SEND MESSAGE time: ',time);

				time += 50;

				if (time > 20000) {
					error.code = "XXXX", error.message = "PostMessage Send Fail";
					alert('서버와 통신할 수 없습니다.'); clearInterval(id);
				}

				if(util.indexOf(sendedIdArray, msgId) > -1) {
					console.log('Clear Interval', id);
					clearInterval(id);
				} else {
					sendPostMaru(obj);
				}
			}

			function sendPostMaru(obj) {
				console.log('SEND POSTMESSAGE TO IFRAME:', obj, 'Message ID:', obj.msgId);

				var contentWindow = doc.getElementById("c3_pop_iframe").contentWindow;
				contentWindow.postMessage(JSON.stringify(obj), maruUrl);
			}
		}
	}

	function layerInit() {
		if (layerInited) return
		layerInited = true

		var popOverlay = util.createElementById('c3_pop_overlay');
		popOverlay.setAttribute('style', 'position:fixed;top:0;bottom:0;left:0;right:0;background: rgba(0, 0, 0, 0.5);width:100%;height:100%;filter:progid:DXImageTransform.Microsoft.gradient(startColorstr=#60000000,endColorstr=#60000000);');

		var popOverlayWrap = util.createElementById('c3pop_pop_overlay_wrap');
		popOverlayWrap.setAttribute('style', 'z-index:2147483600;position:absolute;top:0;bottom:0;left:0;right:0;');
		popOverlayWrap.innerHTML += '<style>#c3pop_close_btn:hover {opacity: 1;} #c3pop_close_btn {display: none;  position: absolute;  right: 20px;  top: 20px;  width: 20px; height: 20px; opacity: 0.5; color: white;} #c3pop_close_btn:before, #c3pop_close_btn:after { display:none; position: absolute;  left: 8px;  content: \' \';  height: 20px;  width: 3px;  background-color: white;} #c3pop_close_btn:before {-webkit-transform: rotate(-45deg);-moz-transform: rotate(-45deg);-o-transform: rotate(-45deg);transform: rotate(-45deg);} #c3pop_close_btn:after {-webkit-transform: rotate(45deg);-moz-transform: rotate(45deg);-o-transform: rotate(45deg);transform: rotate(45deg);}</style>';
		popOverlayWrap.appendChild(popOverlay);

		var contentFixed = util.createElementById('c3pop_content_fixed');
		contentFixed.setAttribute('style', 'z-index: 2147483601;position:fixed;top: 0;left: 0;width:100%;height:100%;background-color:transparent;');

		var popCloseBtn = util.createElementById('c3pop_close_btn');

		var popIframe = util.createElementById('c3_pop_iframe', 'iframe');
		popIframe.setAttribute('frameborder', 0);
		popIframe.setAttribute('allowTransparency', true);
		popIframe.setAttribute('style', 'position:fixed;top: 0;left: 0;width:100%;height:100%;');

		contentFixed.appendChild(popIframe);
		contentFixed.appendChild(popCloseBtn);

		doc.body.appendChild(popOverlayWrap);
		doc.body.appendChild(contentFixed);

		popOverlayWrap.style.display = 'none';
		popOverlay.style.display = 'none';
		contentFixed.style.display = 'none';

		if (util.isIE8()) util.addEventListener(contentFixed, 'click', removePop);
		else util.addEventListener(popCloseBtn, 'click', removePop);

		util.addEventListener(win, 'keydown', function (e) {
			if (e.keyCode == 27) removePop();
		});
	}

	function removePop() {
		util.log('removePop open');
		postMessages.authClose();
		util.log('removePop close');
	}

	function layerClosed() {
		doc.getElementById('c3pop_pop_overlay_wrap').style.display = 'none';
		doc.getElementById('c3_pop_overlay').style.display = 'none';
		doc.getElementById('c3pop_content_fixed').style.display = 'none';
		doc.getElementById('c3_pop_iframe').src = 'about:blank';
	}

	function init() {
		util.log('IMPORT CLIENT.JS FILE');

		if (util.isOldIE()) {
			alert('지원하지 않는 브라우저 입니다.'); return;
		}
		layerInit();

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
				//layerClosed();
			} else if (recv.type === 'LAYER_VACT_CLOSED') {
				layerClosed();
			} else if (recv.type === 'AUTH_RESULT') {
				authResult(recv.data);
			} else if (recv.type === 'AUTH_VACT_RESULT') {
				authVactResult(recv.data);
			}
		});

		if (util.isIE8()) {
			var onresize = window.onresize;

			window.onresize = function (event) {
				if (typeof onresize === 'function') onresize();
				postMessages.ie8Resize();
			}

			setTimeout(function () {
				postMessages.ie8Resize();
			}, 1000);
		}
	}

	function authResult (data) {
		var url = c3Config.redirectUrl;

		if (url && url.length > 1) {
			console.log('[CLIENT] REDIRECT: ', url);

			var form = doc.createElement('form');
			form.method = 'GET';
			form.action = url;

			var elem = doc.createElement('input');
			//elem.value = JSON.stringify(data);
			elem.value = encodeURI(JSON.stringify(data));
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

	//PYS : 가상계좌발급결과
	function authVactResult (data) {
		//결과값 처리
		var url = MaruConfig.redirectUrl;

		if (url && url.length > 1) {
			console.log('[CLIENT] REDIRECT: ', url);

			var form = doc.createElement('form');
			form.method = 'GET';
			form.action = url;

			var elem = doc.createElement('input');
			//elem.value = JSON.stringify(data);
			elem.value = encodeURI(JSON.stringify(data));
			elem.name = 'result';

			form.appendChild(elem);
			doc.body.appendChild(form);
			form.submit();
		}

		var resFnc = MaruConfig.responseFunction;

		if(resFnc && typeof resFnc == 'function') {
			resFnc(data);
		}
	}

	function setDebug(bool) {
		debug = bool;
	}

	var KWON = {
		util: util,
		debug: setDebug,
		c3pop: c3pop,
		removec3pop: removePop,
		onlyAuth: onlyAuth
	}

	util.documentReady(init)
	window.KWON = KWON
	return KWON
})(window, document);