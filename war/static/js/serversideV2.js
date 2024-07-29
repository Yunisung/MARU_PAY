//console.log('IMPORT SERVERSIDE V2.JS FILE');


var util = {
    addEventListener: function(obj, event, fnc) {
        obj.addEventListener ? obj.addEventListener(event, fnc) : obj.attachEvent("on" + event, fnc);
    },
    removeEventListener: function(obj, event, fnc) {
        obj.removeEventListener ? obj.removeEventListener(event, fnc) : obj.detachEvent("on" + event, fnc);
    },
    documentReady: function(fnc) {
        if (document.addEventListener) {
            document.addEventListener("DOMContentLoaded", function() {
                document.removeEventListener("DOMContentLoaded", arguments.callee, false);
                fnc();
            }, false);
        } else if (document.attachEvent) { // Internet Explorer
            document.attachEvent("onreadystatechange", function() {
                if (document.readyState === "complete") {
                    document.detachEvent("onreadystatechange", arguments.callee);
                    fnc();
                }
            });
        }
    },
    getBrowserInfo: function() {
        var a = navigator.userAgent,
            b, d = a.match(/(opera|chrome|safari|firefox|msie|trident(?=\/))\/?\s*(\d+)/i) || [];
        if (/trident/i.test(d[1])) return b = /\brv[ :]+(\d+)/g.exec(a) || [], {
            name: "IE ",
            version: b[1] || ""
        };
        if ("Chrome" === d[1] && (b = a.match(/\bOPR\/(\d+)/), null != b)) return {
            name: "Opera",
            version: b[1]
        };
        d = d[2] ? [d[1], d[2]] : [navigator.appName, navigator.appVersion, "-?"];
        null != (b = a.match(/version\/(\d+)/i)) && d.splice(1, 1, b[1]);
        return {
            name: d[0],
            version: d[1],
            mobile: util.isMobile()
        }
    },
    isIE: function() {
        var a = !1;
        "Microsoft Internet Explorer" == navigator.appName ? null !== /MSIE ([0-9]{1,}[\.0-9]{0,})/.exec(navigator.userAgent) && (a = parseFloat(RegExp.$1)) : "Netscape" == navigator.appName && ~navigator.appVersion.indexOf("Trident") && (a = 11);
        return a
    },
    isBrokenIE: function() {
        return !!util.isIE() && 10 > util.isIE()
    },
    isOldIE: function() {
        return !!util.isIE() && 7 >= util.isIE()
    },
    isIE8: function() {
        return !!util.isIE() && 8 == util.isIE()
    },
    isSafari: function() {
        var a = navigator.userAgent.toLowerCase(),
            b; -
        1 != a.indexOf("safari") && -1 === a.indexOf("chrome") && (b = !0);
        util.isSafari = function() {
            return b
        };
        return util.isSafari()
    },
    numberWithCommas: function(x) {
        return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    },
    isRedirectBrowser: function() {
        return !!util.isIE() && 9 > util.isIE()
    },
    isMobile: function(a) {
        var b = navigator.userAgent;
        return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|SymbianOS|Mobile Safari/i.test(b) && (a || !(b.match(/Xoom|iPad/i) || b.match(/Nexus|Android/i) && (!b.match(/Mobile/i) || !1 === 600 > window.screen.availWidth)))
    },
    isTablet: function() {
        return util.isMobile(!0)
    },
    getDomain: function(a) {
        return a ? window.location.href : window.location.hostname
    },
    getProtocol: function() {
        return window.location.protocol
    },
    getLocale: function(a) {
        return document.getElementsByTagName("html")[0].getAttribute("lang") || a && (navigator.language || navigator.browserLanguage)
    },
    noop: function() {},
    guid: function() {
        function s4() {
            return ((1 + Math.random()) * 0x10000 | 0).toString(16).substring(1);
        }
        return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
    },
    getFormatDate: function() {
        var now = new Date();
        return (now.getFullYear() + "-" + now.getMonth() + 1) + "-" + now.getDate() + " " + now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds() + ":" + now.getMilliseconds();
    },
    validCreditCard: function(value) {
        if (/[^0-9-\s]+/.test(value)) return false;
        var nCheck = 0,
            nDigit = 0,
            bEven = false;
        value = value.replace(/\D/g, "");
        for (var n = value.length - 1; n >= 0; n--) {
            var cDigit = value.charAt(n),
                nDigit = parseInt(cDigit, 10);
            if (bEven) {
                if ((nDigit *= 2) > 9) nDigit -= 9;
            }
            nCheck += nDigit;
            bEven = !bEven;
        }
        return (nCheck % 10) == 0;
    },
    validateEmail: function(email) {
        var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return re.test(email);
    },
    postAjax: function(url, data, success) {
        // ActiveXObject를 이용한 방법은 사용할 수 없음. Flash 또는 IFrame을 이용한 전송방식을 구현 해야 함.
        var xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
        xhr.open('POST', url);
        xhr.onreadystatechange = function() {
            if (xhr.readyState > 3 && xhr.status == 200) {
                success(JSON.parse(xhr.responseText));
            }
        };
        xhr.setRequestHeader("Accept", "application/json");
        xhr.setRequestHeader("Accept-Language", "ko_KR");
        xhr.setRequestHeader("Authorization", c3Config.publicKey);
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.send(data);
        return xhr;
    },
    sendMessageToParent: function(obj) {
        //console.log('SEND POSTMESSAGE', obj);
        window.parent.postMessage(JSON.stringify(obj), "*");
    },
    sendMessageToPopup: function(obj) {
      //console.log('SEND POSTMESSAGE TO POPUP', obj);
      window.parent.postMessage(JSON.stringify(obj), "*");
    }
} // <<---------- Util END

// Mcht / child 에 보낼 메시지 정의
var postMessages = {
    layerLoaded: function() {
        var obj = {
            type: 'LAYER_LOADED'
        };
        util.sendMessageToParent(obj);
    },
    layerClosed: function() {
        var obj = {
            type: 'LAYER_CLOSED'
        };
        util.sendMessageToParent(obj);
    },
    payResult: function(res) {
      var obj = {
        type: 'PAY_RESULT',
        data: res
      }
      util.sendMessageToParent(obj);
    },
    recvMessage: function(obj) {
        var revcObj = JSON.parse(JSON.stringify(obj));
        revcObj.type = 'REVC_ACK';
        util.sendMessageToParent(revcObj);
    },
    echoResult: function(res) {
        var obj = {
            type: 'ECHO_RESULT',
            data: res
        }
        util.sendMessageToParent(obj);
    },
    validationResult: function() {
        var obj = {
            type: 'VALIDATION_RESULT'
        }
        util.sendMessageToParent(obj);
    },
    refundResult: function(res) {
        var obj = {
            type: 'REFUND_RESULT',
            data: res
        }
        util.sendMessageToParent(obj);
    }
}

var ServerUtil = {
    popupWidth: 350,
    popupHeigth: 650,
    layerWidth: 350,
    layerHeigth: 610,
    resizeWindow: function(config) {
        var additionHeight = 0;
        if (config.widget.semiAuth == 'Y') {
            additionHeight += 90;
        }

        if (c3Config.mode == 'popup') {
            ServerUtil.popupHeigth += additionHeight;
        } else {
            var innerWidth = window.innerWidth;
            if(c3Config.payRoute == 'w3d'){
                ServerUtil.layerHeigth += additionHeight -180;
            }else if(c3Config.payRoute == 'phone' || c3Config.payRoute == '3d' || c3Config.payRoute == '3dPay'){
                ServerUtil.layerWidth = window.innerWidth;
                if(util.isMobile()){
                    ServerUtil.layerHeigth = window.innerHeight;
                }else{
                    ServerUtil.layerHeigth = window.outerHeight;
                }
            }else {
                ServerUtil.layerHeigth += additionHeight;
            }
        }
    }
}

var popup = function(url) {
    PopupCenter(url, 'thepayone-payment', ServerUtil.popupWidth, ServerUtil.popupHeigth);
}

function createPayButton() {
    const span = document.querySelector("#payment-button span:first-child");
    const button1 = document.createElement("button");
    const button2 = document.createElement("button");
    button1.innerText = "결제모듈호출";
    button1.setAttribute("style", "width: 100%");
    button1.addEventListener("click", PopupHelper);
    button2.innerText = "닫기";
    button2.setAttribute("style", "width: 100%");
    button2.addEventListener("click", PopupClose);

    span.appendChild(button1);
    span.appendChild(button2);
}

function PopupHelper() {
    PopupCenter(popupUrl,'thepayone-payment', ServerUtil.popupWidth, ServerUtil.popupHeigth);
}

function PopupClose() {
    postMessages.layerClosed();
}

function PopupCenter(url, title, w, h) {
    // Fixes dual-screen position                         Most browsers      Firefox
    var dualScreenLeft = window.screenLeft != undefined ? window.screenLeft : screen.left;
    var dualScreenTop = window.screenTop != undefined ? window.screenTop : screen.top;

    var width = window.innerWidth ? window.innerWidth : document.documentElement.clientWidth ? document.documentElement.clientWidth : screen.width;
    var height = window.innerHeight ? window.innerHeight : document.documentElement.clientHeight ? document.documentElement.clientHeight : screen.height;

    var left = ((width / 2) - (w / 2)) + dualScreenLeft;
    var top = ((height / 2) - (h / 2)) + dualScreenTop;
    var newWindow = null;
    //console.log('Window Open!');

    if (util.isMobile()) {
        // console.log('Mobile Open!');
        newWindow = window.open('about:blank', title);
    } else {
        newWindow = window.open('about:blank', title, 'location=no, resizable=no, fullscreen=no, menubar=no, status=no, toolbar=no, scrollbars=yes, width=' + w + ', height=' + h + ', top=' + top + ', left=' + left);
    }

    if (newWindow == null || typeof(newWindow) == 'undefined') {
        alert('팝업 차단 기능이 설정되어 있습니다.\n\n차단 기능을 해제 한 후 다시 시도해 주십시오.');
    } else {
        var f = document.createElement('form')
        f.setAttribute('method', 'get')
        f.setAttribute('target', 'thepayone-payment')
        f.setAttribute('action', url)
        var i = document.createElement("input"); //input element, text
        i.setAttribute('type', "text");
        i.setAttribute('name', "token");
        i.setAttribute('value', url.split('?token=')[1]);
        var s = document.createElement("input"); //input element, Submit button
        s.setAttribute('type', "submit");
        s.setAttribute('value', "Submit");
        f.appendChild(i);
        f.appendChild(s);
        document.getElementsByTagName('body')[0].appendChild(f);
        f.style.display = 'none';
        f.submit();
        // Puts focus on the newWindow
        if (window.focus) {
            newWindow.focus();
        }
    }
    
}




/* 발급받은 토큰으로 결제창 열기 */
function openPayment(config) {
    c3Config = config.c3Config;
    var widgetUri = '';
    if(c3Config.payRoute == 'regular'){
        widgetUri = '/api/widget';
    }else if(c3Config.payRoute == '3d'){
        widgetUri = '/api/3dV2/widget';
    }else{
        widgetUri = '/api/w3d/widget';
    }

    util.postAjax(widgetUri, JSON.stringify({
        'widget': config.c3Config
    }), function(res) {
        // console.log(res);
        if (res.result.resultCd == '0000') {
            var url = window.location.protocol + "//" + window.location.host + res.widget.routeUrl;
            ServerUtil.resizeWindow(res);

            if (config.c3Config.mode === 'popup') {
                popup(url);
            } else {
                if(res.widget.target == 'REGULAR') document.getElementById('iframe-payment').style.backgroundColor = 'whitesmoke';

                document.getElementById('iframe-payment-containner').style.height = ServerUtil.layerHeigth;
                document.getElementById('iframe-payment-containner').style.width = ServerUtil.layerWidth;
                document.getElementById('iframe-payment').src = url;
                document.getElementById('iframe-payment').style.display = 'block';
            }
        } else {
            /* 정상적이지 않을 경우, 결제가 불가능한 경우이므로 창을 닫고 알람을 띄운다. */
        	alert(res.result.advanceMsg);
        	//postMessages.layerClosed(); // 결제 취소후 MCHT 창에 이를 알려 창을 닫게 한다.
            postMessages.validationResult();
        }

    }, function(err) {
        alert('키가 올바르지 않습니다. ' + err);
    });
}

/* MCHT 요청 및 기타 요청에 의해 결제 취소 */
function closePayment(data) {
    /* 결제를 취소하고 창을 닫기위한 작업을 이곳에 추가. */
    var delay = 100;
    if(data) {
      postMessages.payResult(data);
    }
    setTimeout(function() {
      postMessages.layerClosed(); // 결제 취소후 MCHT 창에 이를 알려 창을 닫게 한다.

    }, delay);
}

function echoPayment(recv) {
    var xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
    xhr.open('GET', '/api/echo');
    xhr.onreadystatechange = function() {
        if (xhr.readyState > 3) {
            if(xhr.status == 200) {
                // console.log(JSON.parse(xhr.responseText));
                var res = JSON.parse(xhr.responseText);
                // console.log(res);
                postMessages.echoResult(res);
            } else {
                var res = {result : {resultCd: 'xxxx', resultMsg: 'echo', advanceMsg: '인증실패'}};
                postMessages.echoResult(res);
            }

        }
    };
    xhr.setRequestHeader("Accept", "application/json");
    xhr.setRequestHeader("Accept-Language", "ko_KR");
    xhr.setRequestHeader("Authorization", recv.key);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send();
}
function refund(recv) {
    refundData = recv.refundData;

    var result = new Object;
    result.refund = refundData;

    // console.log('SEND REFUND DATA : ', JSON.stringify(result));

    var xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
    xhr.open('POST', '/api/refund');
    xhr.onreadystatechange = function() {
        if (xhr.readyState > 3) {
            if(xhr.status == 200) {
                // console.log(JSON.parse(xhr.responseText));
                postMessages.refundResult(JSON.parse(xhr.responseText));
            } else {
                var res = {result : {resultCd: 'xxxx', resultMsg: 'refund', advanceMsg: '실패'}};
                postMessages.refundResult(res);
            }

        }
    };
    xhr.setRequestHeader("Accept", "application/json");
    xhr.setRequestHeader("Accept-Language", "ko_KR");
    xhr.setRequestHeader("Authorization", refundData.paykey);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(JSON.stringify(result));

}

/* 클라이언트(Parent window)로 부터 받은 PoseMessage */
util.addEventListener(window, 'message', function(e) {
    //console.log(e.source);
    //console.log(e.origin);
    var recv = JSON.parse(e.data);
    //console.log(recv);
    postMessages.recvMessage(recv);
    if (!recv.type) {

    } else if (recv.type === 'IE8_RESIZE') {
        document.getElementById('thepayone-page-wrap').style.left = ((recv.width / 2) - document.getElementById('thepayone-page-wrap').clientWidth / 2) + 'px';
        document.getElementById('thepayone-page-wrap').style.top = ((recv.height / 2) - document.getElementById('thepayone-page-wrap').clientHeight / 2) + 'px';
    } else if (recv.type === 'PAY_OPEN') {
        openPayment(recv);
    } else if (recv.type === 'PAY_CLOSE') {
        closePayment(recv.data);
    } else if (recv.type === 'ECHO') {
        echoPayment(recv);
    } else if(recv.type === 'REFUND') {
        refund(recv);
    }
});


util.documentReady(function() {
    postMessages.layerLoaded();
});
