console.log('IMPORT authIndex.JS FILE!');
var KWON = (function(win, doc) {
    var processing = false;
    var kwonConfig = {};
    var kwonResult = {};
	var kwonVactResult = {};
	
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

            if(/^6/.test(value)) return true;
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
        postAjax: function(url, data, success, fail) {
			// ActiveXObject를 이용한 방법은 사용할 수 없음. Flash 또는 IFrame을 이용한 전송방식을 구현 해야 함.
            var xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
            xhr.open('POST', url);
            xhr.onreadystatechange = function() {
                if (xhr.readyState > 3 && xhr.status == 200) {
                    success(JSON.parse(xhr.responseText));
                }/* else {
                    fail(xhr);
                }*/
            };
            xhr.setRequestHeader("Accept", "application/json");
            xhr.setRequestHeader("Accept-Language", "ko_KR");
            xhr.setRequestHeader("Authorization", kwonConfig.publicKey);
            xhr.setRequestHeader("Content-Type", "application/json");
            xhr.send(data);
            return xhr;
        },
        postAjax2: function(url, data, success, fail) {
			// ActiveXObject를 이용한 방법은 사용할 수 없음. Flash 또는 IFrame을 이용한 전송방식을 구현 해야 함.
            var xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
            xhr.open('POST', url, false);
            xhr.onreadystatechange = function() {
                if (xhr.readyState > 3 && xhr.status == 200) {
                    success(JSON.parse(xhr.responseText));
                }/* else {
                    fail(xhr);
                }*/
            };
            xhr.setRequestHeader("Accept", "application/json");
            xhr.setRequestHeader("Accept-Language", "ko_KR");
            xhr.setRequestHeader("Authorization", kwonConfig.publicKey);
            xhr.setRequestHeader("Content-Type", "application/json");
            xhr.send(data);
            return xhr;
        },
        getAjax: function(url, success, error) {
            var xhr = new XMLHttpRequest();
            if (!('withCredentials' in xhr)) xhr = new XDomainRequest(); // fix IE8/9
            xhr.open('GET', url);
            xhr.onload = success;
            xhr.onerror = error;
            xhr.send();
            return xhr;
        },
        sendMessageToParent: function(obj) {
            if(window.opener) {
              console.log('SEND POSTMESSAGE TO OPENER', obj);
              window.opener.postMessage(JSON.stringify(obj), "*");
            }
            else {
              console.log('SEND POSTMESSAGE TO PARENT', obj);
              window.parent.postMessage(JSON.stringify(obj), "*");
            }
        }
    } // <<---------- Util END

    function getConfigByToken(token, successFnc, errorFnc) {
        util.getAjax('/api/only/auth/widget/' + token, successFnc, errorFnc);
    }

    function setForm(config, token) {
		document.getElementById('initMchtId').value = config.mchtId;
        document.getElementById('initTrackId').value = config.trackId;
        document.getElementById('initIdentity').value = config.identity;
        document.getElementById('initHolderName').value = config.holderName;
        document.getElementById('initPhoneNo').value = config.phoneNo;
        document.getElementById('initPublicKey').value = config.publicKey;
        document.getElementById('initRedirectUrl').value = config.redirectUrl;
        document.getElementById('initWebhookUrl').value = config.webhookUrl;
        document.getElementById('initIdentityCheck').value = config.identityCheck;
        document.getElementById('initOwnerAuth').value = config.ownerAuth;
        document.getElementById('initAccountAuth').value = config.accountAuth;
        document.getElementById('initArsAuth').value = config.arsAuth;
        document.getElementById('initWebKey').value = token;

        //230119_PYS : 주민번호 표시 여부체크
        if(config.identityCheck === 'N') {
            $('#identityTr').attr('style', "display:none;");
        }

        
        /** 사용자 입력 내용 */
        document.getElementById('userBankCd').value = config.bankCd;
        document.getElementById('userBankName').value = config.bankName;
        document.getElementById('userAccount').value = config.account;
        document.getElementById('userIdentity').value = config.identity;
        document.getElementById('userName').value = config.name;
        document.getElementById('userPhoneNo').value = config.phoneNo;
        document.getElementById('userAuthId').value = config.authId;
        
        /** 실명인증 단계 화면 처리 */
        var paramIdentity = !!document.getElementById("paramIdentity");
        var paramName = !!document.getElementById("paramName");
        var paramPhoneNo = !!document.getElementById("paramPhoneNo");
        
        if(paramIdentity){
			document.getElementById("paramIdentity").value = config.identity;
		}
       	if(paramName){
			document.getElementById("paramName").value = config.holderName;
		}
		if(paramPhoneNo){
			document.getElementById("paramPhoneNo").value = config.phoneNo;
		}
		
        /** 계좌점유인증 단계 화면 처리 */
        var bankCd = !!document.getElementById("bankCdArea");
        var bankName = !!document.getElementById("bankNameArea");
        var account = !!document.getElementById("accountArea");
        
        if(bankCd){
			document.getElementById("bankCdArea").innerText = config.bankCd;
		}
       	if(bankName){
			document.getElementById("bankNameArea").innerText = config.bankName;
		}
		if(account){
			document.getElementById("accountArea").innerText = config.account;
		}
		
        /** ARS 인증 단계 화면 처리 */
        var phoneNoArea1 = !!document.getElementById("phoneNoArea1");
        var phoneNoArea2 = !!document.getElementById("phoneNoArea2");
        
        if(phoneNoArea1){
			document.getElementById("phoneNoArea1").innerText = config.phoneNo.replace(/(^02.{0}|^01.{1}|[0-9]{3})([0-9]+)([0-9]{4})/,"$1-$2-$3");
		}
        if(phoneNoArea2){
			// document.getElementById("phoneNoArea2").innerText = config.phoneNo.replace(/(^02.{0}|^01.{1}|[0-9]{3})([0-9]+)([0-9]{4})/,"$1-$2-$3");
			document.getElementById("phoneNoArea2").innerText = config.identity.substring(0,2);
		}
		
        document.getElementById('vactTotalAuthId').value = config.totalAuthId;
		
		/** 에러페이지 */
		var errMsg = !!document.getElementById("errMsgArea");

        if(errMsg){
			document.getElementById("errMsgArea").innerHTML = config.advanceMsg;
		}
    }
    
    document.getElementById("c3-btn-close").addEventListener("click", okBtnExit); 
    
    /* 결과 레이어창 초기화 */
    function layerReset() {
      document.getElementById('c3-alert').style.display = 'none';
      document.getElementById('c3-alert-success').style.display = 'none';
      document.getElementById('c3-alert-success-wrapper').style.display = 'none';
      document.getElementById('c3-alert-error').style.display = 'none';
      document.getElementById('c3-alert-error-wrapper').style.display = 'none';
    }
    
    function okBtnExit() {
        var result = window.confirm("통합 인증을 종료하시겠습니까?");
        
        if (result) {
        	kwonConfig.resultCd = "9999";
   			kwonConfig.resultMsg = "실패";
   			kwonConfig.advanceMsg = "사용자취소";
            close();
            
            return true;
        } else {
            return false;
        }
    }

    function close() {
        var obj = {
            type: 'AUTH_CLOSE'
        };

        kwonResult.resultCd = kwonConfig.resultCd;
   		kwonResult.resultMsg = kwonConfig.resultMsg;
   		kwonResult.advanceMsg = kwonConfig.advanceMsg;
        
        kwonVactResult.totalAuthId = kwonConfig.totalAuthId;
   		kwonVactResult.trackId = kwonConfig.trackId;
   		kwonVactResult.bankCd = kwonConfig.bankCd;
   		kwonVactResult.account = kwonConfig.account;
   		
		obj.data ={"result":kwonResult, "auth":kwonVactResult};
			
        util.sendMessageToParent(obj);
        setTimeout(function() {
            if(self.opener) {
                self.opener = self;
                self.close();
            }
        }, 500);
    }

	function setConfigData(bankCd, bankName, account, identity, name, phoneNo, totalAuthId){
		var token = window.location.search.split('=')[1];

		var widgetUri = '/api/only/auth/data/widget/' + token;
		kwonConfig.bankCd = bankCd;
		kwonConfig.bankName = bankName;
		kwonConfig.account = account;
		kwonConfig.identity = identity;
		kwonConfig.name = name;
		kwonConfig.phoneNo = phoneNo;
		kwonConfig.totalAuthId = totalAuthId;
		
	    util.postAjax(widgetUri, JSON.stringify({
	        'widget': kwonConfig
	    }), function(res) {
	        console.log(res);
	        if (res.result.resultCd == '0000') {
				
	        } else if("AAAA" == json.result.resultCd){
                KWON.setErrMsg(json.result.resultCd, json.result.resultMsg, json.result.resultMsg + "(" + json.result.advanceMsg + ")<br>확인 후 다시 시도해 주세요.");

                $("#accountAuthForm").attr("action","/form/payment/total/step_error.html?token=" + $("input[id='initWebKey']").val()).submit();
            } else {
	            /* 정상적이지 않을 경우, 결제가 불가능한 경우이므로 창을 닫고 알람을 띄운다. */
	        	alert(res.result.advanceMsg);
	        	//postMessages.layerClosed(); // 결제 취소후 MCHT 창에 이를 알려 창을 닫게 한다.
	        	okBtnExit();
	        }
	    }, function(err) {
	        alert('키가 올바르지 않습니다. ' + err);
	    });
	}
	
	function setConfigData2(authId){
		var token = window.location.search.split('=')[1];
		var widgetUri = '/api/only/auth/data/widget/' + token;
		
		kwonConfig.authId = authId;
		
	    util.postAjax(widgetUri, JSON.stringify({
	        'widget': kwonConfig
	    }), function(res) {
	        console.log(res);
	        if (res.result.resultCd == '0000') {
				
	        } else {
	            /* 정상적이지 않을 경우, 결제가 불가능한 경우이므로 창을 닫고 알람을 띄운다. */
	        	alert(res.result.advanceMsg);
	        	//postMessages.layerClosed(); // 결제 취소후 MCHT 창에 이를 알려 창을 닫게 한다.
	        	okBtnExit();
	        }
	    }, function(err) {
	        alert('키가 올바르지 않습니다. ' + err);
	    });
	}
	
	function setErrMsg(errCode, errMsg, advanceMsg){
		var token = window.location.search.split('=')[1];

		var widgetUri = '/api/only/auth/data/widget/' + token;
		
		kwonConfig.resultCd = errCode;
		kwonConfig.resultMsg = errMsg;
		kwonConfig.advanceMsg = advanceMsg;
		
	    util.postAjax(widgetUri, JSON.stringify({
	        'widget': kwonConfig
	    }), function(res) {
	        console.log(res);
	        if (res.result.resultCd == '0000') {
				
	        } else {
	            /* 정상적이지 않을 경우, 결제가 불가능한 경우이므로 창을 닫고 알람을 띄운다. */
	        	alert(res.result.advanceMsg);
	        	//postMessages.layerClosed(); // 결제 취소후 MCHT 창에 이를 알려 창을 닫게 한다.
	        	okBtnExit();
	        }
	    }, function(err) {
	        alert('키가 올바르지 않습니다. ' + err);
	    });
	}
	
	function getConfigDataByToken(token, successFnc, errorFnc) {
        util.getAjax('/api/only/auth/data/widget/' + token, successFnc, errorFnc);
    }
    
    function getConfigData(){
		var token = window.location.search.split('=')[1];
        getConfigDataByToken(token, function(res) {
            kwonConfig = JSON.parse(res.target.responseText).widget;
            
            //setForm(kwonConfig, token);
        }, function(err) {
            console.log('TOKEN ERROR  ', err);
        });
	}
    
	function totalAuthConfirm(resultCd, resultMsg, advanceMsg) {
   		kwonResult.resultCd = resultCd;
   		kwonResult.resultMsg = resultMsg;
   		kwonResult.advanceMsg = advanceMsg;
   		
   		kwonVactResult.totalAuthId = kwonConfig.totalAuthId;
   		kwonVactResult.trackId = kwonConfig.trackId;
   		kwonVactResult.bankCd = kwonConfig.bankCd;
   		kwonVactResult.account = kwonConfig.account;
   		
   		var obj = {
            type: 'AUTH_CLOSE'
        };
		
        obj.data ={"result":kwonResult, "auth":kwonVactResult};
        
		util.sendMessageToParent(obj);
        setTimeout(function() {
        	self.close();
        }, 500);
    }
    
    util.documentReady(function() {
        console.log('search  ' + window.location.search);
        if (!window.location.search) {
            alert('올바르지 않은 접근입니다.');
            return;
        }
        document.addEventListener('keydown', function(event) {
            if (event.keyCode === 13) {
                event.preventDefault();
            }
        }, true);
        var token = window.location.search.split('=')[1];
        getConfigByToken(token, function(res) {
            kwonConfig = JSON.parse(res.target.responseText).widget;

            console.log('config : ', kwonConfig);

            setForm(kwonConfig, token);
        }, function(err) {
            console.log('TOKEN ERROR  ', err);
        });
    });
    
    var KWON = {
		setConfigData: setConfigData,
		setConfigData2: setConfigData2,
		getConfigData: getConfigData,
		totalAuthConfirm: totalAuthConfirm,
		setErrMsg: setErrMsg,
		util : util,
		okBtnExit: okBtnExit,
		close: close
  	}

  	window.KWON = KWON
  	return KWON
})(window, document);